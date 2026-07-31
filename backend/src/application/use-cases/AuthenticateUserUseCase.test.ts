import { beforeEach, describe, expect, it } from "vitest";
import { AuthenticateUserUseCase } from "./AuthenticateUserUseCase";
import { RegisterUserUseCase } from "./RegisterUserUseCase";
import { EnrollMfaUseCase } from "./EnrollMfaUseCase";
import { ConfirmMfaEnrollmentUseCase } from "./ConfirmMfaEnrollmentUseCase";
import { SessionIssuer } from "../services/SessionIssuer";
import { MfaChallengeIssuer } from "../services/MfaChallengeIssuer";
import { InMemoryUserRepository } from "../../adapters/out/persistence/InMemoryUserRepository";
import { InMemoryRefreshTokenRepository } from "../../adapters/out/persistence/InMemoryRefreshTokenRepository";
import { InMemoryMfaCredentialRepository } from "../../adapters/out/persistence/InMemoryMfaCredentialRepository";
import { InMemoryMfaChallengeRepository } from "../../adapters/out/persistence/InMemoryMfaChallengeRepository";
import { InvalidCredentialsError } from "../../domain/user/errors/InvalidCredentialsError";
import { Email } from "../../domain/user/Email";
import { FakePasswordHasher } from "../../test-support/FakePasswordHasher";
import { SequentialIdGenerator } from "../../test-support/SequentialIdGenerator";
import { FakeTokenService } from "../../test-support/FakeTokenService";
import { SequentialRefreshTokenGenerator } from "../../test-support/SequentialRefreshTokenGenerator";
import { SequentialMfaChallengeGenerator } from "../../test-support/SequentialMfaChallengeGenerator";
import { FakeTotpService } from "../../test-support/FakeTotpService";
import { FixedClock } from "../../test-support/FixedClock";

const REFRESH_TOKEN_TTL_MS = 7 * 24 * 60 * 60 * 1000;
const CHALLENGE_TTL_MS = 5 * 60 * 1000;
const NOW = new Date("2026-01-01T00:00:00.000Z");

describe("AuthenticateUserUseCase", () => {
  let userRepository: InMemoryUserRepository;
  let passwordHasher: FakePasswordHasher;
  let mfaCredentialRepository: InMemoryMfaCredentialRepository;
  let useCase: AuthenticateUserUseCase;

  beforeEach(async () => {
    userRepository = new InMemoryUserRepository();
    passwordHasher = new FakePasswordHasher();
    mfaCredentialRepository = new InMemoryMfaCredentialRepository();

    const sessionIssuer = new SessionIssuer(
      new FakeTokenService(),
      new InMemoryRefreshTokenRepository(),
      new SequentialRefreshTokenGenerator(),
      new SequentialIdGenerator("session"),
      new FixedClock(NOW),
      REFRESH_TOKEN_TTL_MS,
    );
    const mfaChallengeIssuer = new MfaChallengeIssuer(
      new InMemoryMfaChallengeRepository(),
      new SequentialMfaChallengeGenerator(),
      new SequentialIdGenerator("challenge"),
      new FixedClock(NOW),
      CHALLENGE_TTL_MS,
    );
    useCase = new AuthenticateUserUseCase(userRepository, passwordHasher, sessionIssuer, mfaCredentialRepository, mfaChallengeIssuer);

    const registerUser = new RegisterUserUseCase(userRepository, passwordHasher, new SequentialIdGenerator("user"));
    await registerUser.execute({ email: "user@example.com", password: "StrongPass1" });
  });

  it("issues an access token and a refresh token for valid credentials when MFA is not active (RF-003, RF-008)", async () => {
    const result = await useCase.execute({ email: "user@example.com", password: "StrongPass1" });

    expect(result).toEqual({ mfaRequired: false, token: "token-for-user-1", refreshToken: "refresh-token-1" });
  });

  it("rejects authentication for a non-existent email", async () => {
    await expect(useCase.execute({ email: "ghost@example.com", password: "whatever1" })).rejects.toThrow(
      InvalidCredentialsError,
    );
  });

  it("rejects authentication with a wrong password using the same error as an unknown email", async () => {
    // Same error type for "email not found" and "wrong password" on purpose:
    // prevents an attacker from using the login endpoint to enumerate registered emails.
    await expect(useCase.execute({ email: "user@example.com", password: "WrongPass1" })).rejects.toThrow(
      InvalidCredentialsError,
    );
  });

  it("rejects login for an anonymized (deleted) account, as an explicit invariant (RF-007, ADR-0010)", async () => {
    // In practice DeleteAccountUseCase also rewrites the email, which alone would block this
    // lookup — this test isolates the isDeleted() guard itself as a defense-in-depth check,
    // independent of that side effect.
    const email = Email.create("user@example.com");
    const user = await userRepository.findByEmail(email);
    await userRepository.update(
      user!.anonymize({ email, passwordHash: "unusable-hash", deletedAt: new Date("2026-02-01T00:00:00.000Z") }),
    );

    await expect(useCase.execute({ email: "user@example.com", password: "StrongPass1" })).rejects.toThrow(
      InvalidCredentialsError,
    );
  });

  describe("when the account has MFA active (RF-004, ADR-0012)", () => {
    beforeEach(async () => {
      const enroll = new EnrollMfaUseCase(
        userRepository,
        passwordHasher,
        mfaCredentialRepository,
        new FakeTotpService(),
        new SequentialIdGenerator("mfa"),
        new FixedClock(NOW),
      );
      await enroll.execute({ userId: "user-1", password: "StrongPass1" });
      const confirm = new ConfirmMfaEnrollmentUseCase(userRepository, mfaCredentialRepository, new FakeTotpService(), new FixedClock(NOW));
      await confirm.execute({ userId: "user-1", code: "valid-code-for-FAKE_SECRET" });
    });

    it("returns a challenge instead of session tokens after correct credentials", async () => {
      const result = await useCase.execute({ email: "user@example.com", password: "StrongPass1" });

      expect(result).toEqual({ mfaRequired: true, challengeToken: "mfa-challenge-1" });
    });

    it("still rejects wrong credentials before ever considering MFA", async () => {
      await expect(useCase.execute({ email: "user@example.com", password: "WrongPass1" })).rejects.toThrow(
        InvalidCredentialsError,
      );
    });
  });
});
