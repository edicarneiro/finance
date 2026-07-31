import { beforeEach, describe, expect, it } from "vitest";
import { DeleteAccountUseCase } from "./DeleteAccountUseCase";
import { RegisterUserUseCase } from "./RegisterUserUseCase";
import { AuthenticateUserUseCase } from "./AuthenticateUserUseCase";
import { RequestPasswordResetUseCase } from "./RequestPasswordResetUseCase";
import { EnrollMfaUseCase } from "./EnrollMfaUseCase";
import { ConfirmMfaEnrollmentUseCase } from "./ConfirmMfaEnrollmentUseCase";
import { CompleteMfaLoginUseCase } from "./CompleteMfaLoginUseCase";
import { SessionIssuer } from "../services/SessionIssuer";
import { MfaChallengeIssuer } from "../services/MfaChallengeIssuer";
import { InMemoryUserRepository } from "../../adapters/out/persistence/InMemoryUserRepository";
import { InMemoryRefreshTokenRepository } from "../../adapters/out/persistence/InMemoryRefreshTokenRepository";
import { InMemoryPasswordResetTokenRepository } from "../../adapters/out/persistence/InMemoryPasswordResetTokenRepository";
import { InMemoryMfaCredentialRepository } from "../../adapters/out/persistence/InMemoryMfaCredentialRepository";
import { InMemoryMfaChallengeRepository } from "../../adapters/out/persistence/InMemoryMfaChallengeRepository";
import { InvalidCredentialsError } from "../../domain/user/errors/InvalidCredentialsError";
import { InvalidOrExpiredMfaChallengeError } from "../../domain/user/errors/InvalidOrExpiredMfaChallengeError";
import { FakePasswordHasher } from "../../test-support/FakePasswordHasher";
import { SequentialIdGenerator } from "../../test-support/SequentialIdGenerator";
import { FakeTokenService } from "../../test-support/FakeTokenService";
import { FakeTotpService } from "../../test-support/FakeTotpService";
import { SequentialRefreshTokenGenerator } from "../../test-support/SequentialRefreshTokenGenerator";
import { SequentialPasswordResetTokenGenerator } from "../../test-support/SequentialPasswordResetTokenGenerator";
import { SequentialMfaChallengeGenerator } from "../../test-support/SequentialMfaChallengeGenerator";
import { FakePasswordResetNotifier } from "../../test-support/FakePasswordResetNotifier";
import { FixedClock } from "../../test-support/FixedClock";

const NOW = new Date("2026-01-01T00:00:00.000Z");
const REFRESH_TTL_MS = 7 * 24 * 60 * 60 * 1000;
const RESET_TTL_MS = 60 * 60 * 1000;
const CHALLENGE_TTL_MS = 5 * 60 * 1000;

function buildAuthenticateUserUseCase(
  userRepository: InMemoryUserRepository,
  passwordHasher: FakePasswordHasher,
  refreshTokenRepository: InMemoryRefreshTokenRepository,
): AuthenticateUserUseCase {
  const sessionIssuer = new SessionIssuer(
    new FakeTokenService(),
    refreshTokenRepository,
    new SequentialRefreshTokenGenerator(),
    new SequentialIdGenerator("session"),
    new FixedClock(NOW),
    REFRESH_TTL_MS,
  );
  const mfaChallengeIssuer = new MfaChallengeIssuer(
    new InMemoryMfaChallengeRepository(),
    new SequentialMfaChallengeGenerator(),
    new SequentialIdGenerator("challenge"),
    new FixedClock(NOW),
    CHALLENGE_TTL_MS,
  );
  return new AuthenticateUserUseCase(
    userRepository,
    passwordHasher,
    sessionIssuer,
    new InMemoryMfaCredentialRepository(),
    mfaChallengeIssuer,
  );
}

describe("DeleteAccountUseCase", () => {
  let userRepository: InMemoryUserRepository;
  let refreshTokenRepository: InMemoryRefreshTokenRepository;
  let resetTokenRepository: InMemoryPasswordResetTokenRepository;
  let mfaCredentialRepository: InMemoryMfaCredentialRepository;
  let passwordHasher: FakePasswordHasher;
  let useCase: DeleteAccountUseCase;

  beforeEach(async () => {
    userRepository = new InMemoryUserRepository();
    refreshTokenRepository = new InMemoryRefreshTokenRepository();
    resetTokenRepository = new InMemoryPasswordResetTokenRepository();
    mfaCredentialRepository = new InMemoryMfaCredentialRepository();
    passwordHasher = new FakePasswordHasher();

    const registerUser = new RegisterUserUseCase(userRepository, passwordHasher, new SequentialIdGenerator("user"));
    await registerUser.execute({ email: "user@example.com", password: "StrongPass1" });

    useCase = new DeleteAccountUseCase(
      userRepository,
      passwordHasher,
      refreshTokenRepository,
      resetTokenRepository,
      mfaCredentialRepository,
      new SequentialIdGenerator("anon"),
      new FixedClock(NOW),
    );
  });

  it("anonymizes the account when the confirmation password is correct (RF-007)", async () => {
    await useCase.execute({ userId: "user-1", password: "StrongPass1" });

    const user = await userRepository.findById("user-1");
    expect(user?.isDeleted()).toBe(true);
    expect(user?.name).toBeNull();
    expect(user?.email.toString()).not.toBe("user@example.com");
    expect(user?.email.toString()).toContain("@anonymized.financepulse.internal");
  });

  it("rejects deletion with an incorrect confirmation password, leaving the account intact", async () => {
    await expect(useCase.execute({ userId: "user-1", password: "WrongPass1" })).rejects.toThrow(
      InvalidCredentialsError,
    );

    const user = await userRepository.findById("user-1");
    expect(user?.isDeleted()).toBe(false);
  });

  it("blocks future login attempts after deletion", async () => {
    const authenticate = buildAuthenticateUserUseCase(userRepository, passwordHasher, refreshTokenRepository);

    await useCase.execute({ userId: "user-1", password: "StrongPass1" });

    await expect(authenticate.execute({ email: "user@example.com", password: "StrongPass1" })).rejects.toThrow(
      InvalidCredentialsError,
    );
  });

  it("revokes every active refresh token of the user upon deletion", async () => {
    const authenticate = buildAuthenticateUserUseCase(userRepository, passwordHasher, refreshTokenRepository);
    const result = await authenticate.execute({ email: "user@example.com", password: "StrongPass1" });
    if (result.mfaRequired) {
      throw new Error("Test setup error: MFA should not be active for this user.");
    }

    await useCase.execute({ userId: "user-1", password: "StrongPass1" });

    const session = await refreshTokenRepository.findByRawToken(result.refreshToken);
    expect(session?.isRevoked()).toBe(true);
  });

  it("invalidates any outstanding password reset token of the user upon deletion", async () => {
    const requestReset = new RequestPasswordResetUseCase(
      userRepository,
      resetTokenRepository,
      new SequentialPasswordResetTokenGenerator(),
      new SequentialIdGenerator("reset"),
      new FixedClock(NOW),
      new FakePasswordResetNotifier(),
      RESET_TTL_MS,
    );
    await requestReset.execute({ email: "user@example.com" });

    await useCase.execute({ userId: "user-1", password: "StrongPass1" });

    const resetToken = await resetTokenRepository.findByRawToken("reset-token-1");
    expect(resetToken?.isValid(NOW)).toBe(false);
  });

  it("disables any active MFA credential upon deletion, invalidating pending login challenges (ADR-0012)", async () => {
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

    // A login challenge issued *before* deletion — simulates the window where
    // an attacker (or the user) has a still-pending, not-yet-completed MFA
    // challenge when the account gets deleted through a separate valid session.
    const mfaChallengeRepository = new InMemoryMfaChallengeRepository();
    const mfaChallengeIssuer = new MfaChallengeIssuer(
      mfaChallengeRepository,
      new SequentialMfaChallengeGenerator(),
      new SequentialIdGenerator("challenge"),
      new FixedClock(NOW),
      CHALLENGE_TTL_MS,
    );
    const { challengeToken } = await mfaChallengeIssuer.issueFor("user-1");

    await useCase.execute({ userId: "user-1", password: "StrongPass1" });

    const credential = await mfaCredentialRepository.findByUserId("user-1");
    expect(credential?.isActive()).toBe(false);

    // Without the fix, this would succeed and hand out a fresh session for a deleted account.
    const completeMfaLogin = new CompleteMfaLoginUseCase(
      mfaChallengeRepository,
      mfaCredentialRepository,
      new FakeTotpService(),
      new SessionIssuer(
        new FakeTokenService(),
        refreshTokenRepository,
        new SequentialRefreshTokenGenerator(),
        new SequentialIdGenerator("session"),
        new FixedClock(NOW),
        REFRESH_TTL_MS,
      ),
      new FixedClock(NOW),
    );
    await expect(
      completeMfaLogin.execute({ challengeToken, code: "valid-code-for-FAKE_SECRET" }),
    ).rejects.toThrow(InvalidOrExpiredMfaChallengeError);
  });
});
