import { beforeEach, describe, expect, it } from "vitest";
import { ResetPasswordUseCase } from "./ResetPasswordUseCase";
import { RequestPasswordResetUseCase } from "./RequestPasswordResetUseCase";
import { RegisterUserUseCase } from "./RegisterUserUseCase";
import { AuthenticateUserUseCase } from "./AuthenticateUserUseCase";
import { SessionIssuer } from "../services/SessionIssuer";
import { MfaChallengeIssuer } from "../services/MfaChallengeIssuer";
import { InMemoryUserRepository } from "../../adapters/out/persistence/InMemoryUserRepository";
import { InMemoryPasswordResetTokenRepository } from "../../adapters/out/persistence/InMemoryPasswordResetTokenRepository";
import { InMemoryRefreshTokenRepository } from "../../adapters/out/persistence/InMemoryRefreshTokenRepository";
import { InMemoryMfaCredentialRepository } from "../../adapters/out/persistence/InMemoryMfaCredentialRepository";
import { InMemoryMfaChallengeRepository } from "../../adapters/out/persistence/InMemoryMfaChallengeRepository";
import { InvalidOrExpiredResetTokenError } from "../../domain/user/errors/InvalidOrExpiredResetTokenError";
import { WeakPasswordError } from "../../domain/user/errors/WeakPasswordError";
import { FakePasswordHasher } from "../../test-support/FakePasswordHasher";
import { SequentialIdGenerator } from "../../test-support/SequentialIdGenerator";
import { SequentialPasswordResetTokenGenerator } from "../../test-support/SequentialPasswordResetTokenGenerator";
import { SequentialRefreshTokenGenerator } from "../../test-support/SequentialRefreshTokenGenerator";
import { SequentialMfaChallengeGenerator } from "../../test-support/SequentialMfaChallengeGenerator";
import { FakeTokenService } from "../../test-support/FakeTokenService";
import { FakePasswordResetNotifier } from "../../test-support/FakePasswordResetNotifier";
import { FixedClock } from "../../test-support/FixedClock";

const NOW = new Date("2026-01-01T00:00:00.000Z");
const RESET_TTL_MS = 60 * 60 * 1000;
const REFRESH_TTL_MS = 7 * 24 * 60 * 60 * 1000;
const CHALLENGE_TTL_MS = 5 * 60 * 1000;

describe("ResetPasswordUseCase", () => {
  let userRepository: InMemoryUserRepository;
  let resetTokenRepository: InMemoryPasswordResetTokenRepository;
  let refreshTokenRepository: InMemoryRefreshTokenRepository;
  let passwordHasher: FakePasswordHasher;
  let clock: FixedClock;
  let useCase: ResetPasswordUseCase;
  let requestReset: RequestPasswordResetUseCase;

  beforeEach(async () => {
    userRepository = new InMemoryUserRepository();
    resetTokenRepository = new InMemoryPasswordResetTokenRepository();
    refreshTokenRepository = new InMemoryRefreshTokenRepository();
    passwordHasher = new FakePasswordHasher();
    clock = new FixedClock(NOW);

    const registerUser = new RegisterUserUseCase(userRepository, passwordHasher, new SequentialIdGenerator("user"));
    await registerUser.execute({ email: "user@example.com", password: "StrongPass1" });

    requestReset = new RequestPasswordResetUseCase(
      userRepository,
      resetTokenRepository,
      new SequentialPasswordResetTokenGenerator(),
      new SequentialIdGenerator("reset"),
      clock,
      new FakePasswordResetNotifier(),
      RESET_TTL_MS,
    );

    useCase = new ResetPasswordUseCase(resetTokenRepository, userRepository, passwordHasher, refreshTokenRepository, clock);
  });

  it("updates the password when the token is valid (RF-005)", async () => {
    await requestReset.execute({ email: "user@example.com" });

    await useCase.execute({ token: "reset-token-1", newPassword: "NewStrongPass1" });

    const user = await userRepository.findById("user-1");
    expect(user?.passwordHash).toBe("hashed:NewStrongPass1");
  });

  it("marks the reset token as used, rejecting a second use of the same token", async () => {
    await requestReset.execute({ email: "user@example.com" });

    await useCase.execute({ token: "reset-token-1", newPassword: "NewStrongPass1" });

    await expect(useCase.execute({ token: "reset-token-1", newPassword: "AnotherPass1" })).rejects.toThrow(
      InvalidOrExpiredResetTokenError,
    );
  });

  it("revokes every active session of the user upon a successful reset", async () => {
    const sessionIssuer = new SessionIssuer(
      new FakeTokenService(),
      refreshTokenRepository,
      new SequentialRefreshTokenGenerator(),
      new SequentialIdGenerator("session"),
      clock,
      REFRESH_TTL_MS,
    );
    const mfaChallengeIssuer = new MfaChallengeIssuer(
      new InMemoryMfaChallengeRepository(),
      new SequentialMfaChallengeGenerator(),
      new SequentialIdGenerator("challenge"),
      clock,
      CHALLENGE_TTL_MS,
    );
    const authenticate = new AuthenticateUserUseCase(
      userRepository,
      passwordHasher,
      sessionIssuer,
      new InMemoryMfaCredentialRepository(),
      mfaChallengeIssuer,
    );
    const result = await authenticate.execute({ email: "user@example.com", password: "StrongPass1" });
    if (result.mfaRequired) {
      throw new Error("Test setup error: MFA should not be active for this user.");
    }

    await requestReset.execute({ email: "user@example.com" });
    await useCase.execute({ token: "reset-token-1", newPassword: "NewStrongPass1" });

    const session = await refreshTokenRepository.findByRawToken(result.refreshToken);
    expect(session?.isRevoked()).toBe(true);
  });

  it("rejects an expired token", async () => {
    await requestReset.execute({ email: "user@example.com" });
    clock.advanceBy(RESET_TTL_MS + 1);

    await expect(useCase.execute({ token: "reset-token-1", newPassword: "NewStrongPass1" })).rejects.toThrow(
      InvalidOrExpiredResetTokenError,
    );
  });

  it("rejects an unknown token", async () => {
    await expect(useCase.execute({ token: "never-issued", newPassword: "NewStrongPass1" })).rejects.toThrow(
      InvalidOrExpiredResetTokenError,
    );
  });

  it("rejects a weak new password without consuming the token", async () => {
    await requestReset.execute({ email: "user@example.com" });

    await expect(useCase.execute({ token: "reset-token-1", newPassword: "short" })).rejects.toThrow(
      WeakPasswordError,
    );

    // The token must remain valid so the user can retry with a stronger password.
    const stored = await resetTokenRepository.findByRawToken("reset-token-1");
    expect(stored?.isValid(NOW)).toBe(true);
  });
});
