import { beforeEach, describe, expect, it } from "vitest";
import { CompleteMfaLoginUseCase } from "./CompleteMfaLoginUseCase";
import { MfaChallengeIssuer } from "../services/MfaChallengeIssuer";
import { SessionIssuer } from "../services/SessionIssuer";
import { EnrollMfaUseCase } from "./EnrollMfaUseCase";
import { ConfirmMfaEnrollmentUseCase } from "./ConfirmMfaEnrollmentUseCase";
import { RegisterUserUseCase } from "./RegisterUserUseCase";
import { InMemoryUserRepository } from "../../adapters/out/persistence/InMemoryUserRepository";
import { InMemoryMfaCredentialRepository } from "../../adapters/out/persistence/InMemoryMfaCredentialRepository";
import { InMemoryMfaChallengeRepository } from "../../adapters/out/persistence/InMemoryMfaChallengeRepository";
import { InMemoryRefreshTokenRepository } from "../../adapters/out/persistence/InMemoryRefreshTokenRepository";
import { InvalidOrExpiredMfaChallengeError } from "../../domain/user/errors/InvalidOrExpiredMfaChallengeError";
import { InvalidMfaCodeError } from "../../domain/user/errors/InvalidMfaCodeError";
import { FakePasswordHasher } from "../../test-support/FakePasswordHasher";
import { SequentialIdGenerator } from "../../test-support/SequentialIdGenerator";
import { FakeTotpService } from "../../test-support/FakeTotpService";
import { FakeTokenService } from "../../test-support/FakeTokenService";
import { SequentialRefreshTokenGenerator } from "../../test-support/SequentialRefreshTokenGenerator";
import { SequentialMfaChallengeGenerator } from "../../test-support/SequentialMfaChallengeGenerator";
import { FixedClock } from "../../test-support/FixedClock";

const NOW = new Date("2026-01-01T00:00:00.000Z");
const CHALLENGE_TTL_MS = 5 * 60 * 1000;
const REFRESH_TTL_MS = 7 * 24 * 60 * 60 * 1000;

describe("CompleteMfaLoginUseCase", () => {
  let mfaCredentialRepository: InMemoryMfaCredentialRepository;
  let mfaChallengeRepository: InMemoryMfaChallengeRepository;
  let refreshTokenRepository: InMemoryRefreshTokenRepository;
  let clock: FixedClock;
  let mfaChallengeIssuer: MfaChallengeIssuer;
  let useCase: CompleteMfaLoginUseCase;

  beforeEach(async () => {
    const userRepository = new InMemoryUserRepository();
    mfaCredentialRepository = new InMemoryMfaCredentialRepository();
    mfaChallengeRepository = new InMemoryMfaChallengeRepository();
    refreshTokenRepository = new InMemoryRefreshTokenRepository();
    clock = new FixedClock(NOW);

    const registerUser = new RegisterUserUseCase(userRepository, new FakePasswordHasher(), new SequentialIdGenerator("user"));
    await registerUser.execute({ email: "user@example.com", password: "StrongPass1" });

    const enroll = new EnrollMfaUseCase(
      userRepository,
      new FakePasswordHasher(),
      mfaCredentialRepository,
      new FakeTotpService(),
      new SequentialIdGenerator("mfa"),
      clock,
    );
    await enroll.execute({ userId: "user-1", password: "StrongPass1" });
    const confirm = new ConfirmMfaEnrollmentUseCase(userRepository, mfaCredentialRepository, new FakeTotpService(), clock);
    await confirm.execute({ userId: "user-1", code: "valid-code-for-FAKE_SECRET" });

    mfaChallengeIssuer = new MfaChallengeIssuer(
      mfaChallengeRepository,
      new SequentialMfaChallengeGenerator(),
      new SequentialIdGenerator("challenge"),
      clock,
      CHALLENGE_TTL_MS,
    );

    const sessionIssuer = new SessionIssuer(
      new FakeTokenService(),
      refreshTokenRepository,
      new SequentialRefreshTokenGenerator(),
      new SequentialIdGenerator("session"),
      clock,
      REFRESH_TTL_MS,
    );

    useCase = new CompleteMfaLoginUseCase(mfaChallengeRepository, mfaCredentialRepository, new FakeTotpService(), sessionIssuer, clock);
  });

  it("issues session tokens when the challenge and code are valid (RF-004)", async () => {
    const { challengeToken } = await mfaChallengeIssuer.issueFor("user-1");

    const result = await useCase.execute({ challengeToken, code: "valid-code-for-FAKE_SECRET" });

    expect(result.token).toBe("token-for-user-1");
    expect(result.refreshToken).toBe("refresh-token-1");
  });

  it("marks the challenge as used, rejecting a second use of the same challenge", async () => {
    const { challengeToken } = await mfaChallengeIssuer.issueFor("user-1");

    await useCase.execute({ challengeToken, code: "valid-code-for-FAKE_SECRET" });

    await expect(useCase.execute({ challengeToken, code: "valid-code-for-FAKE_SECRET" })).rejects.toThrow(
      InvalidOrExpiredMfaChallengeError,
    );
  });

  it("rejects an unknown challenge token", async () => {
    await expect(useCase.execute({ challengeToken: "never-issued", code: "valid-code-for-FAKE_SECRET" })).rejects.toThrow(
      InvalidOrExpiredMfaChallengeError,
    );
  });

  it("rejects an expired challenge", async () => {
    const { challengeToken } = await mfaChallengeIssuer.issueFor("user-1");
    clock.advanceBy(CHALLENGE_TTL_MS + 1);

    await expect(useCase.execute({ challengeToken, code: "valid-code-for-FAKE_SECRET" })).rejects.toThrow(
      InvalidOrExpiredMfaChallengeError,
    );
  });

  it("rejects an invalid TOTP code without consuming the challenge", async () => {
    const { challengeToken } = await mfaChallengeIssuer.issueFor("user-1");

    await expect(useCase.execute({ challengeToken, code: "000000" })).rejects.toThrow(InvalidMfaCodeError);

    // The challenge must remain valid so the user can retry with the correct code.
    const stored = await mfaChallengeRepository.findByRawToken(challengeToken);
    expect(stored?.isValid(NOW)).toBe(true);
  });
});
