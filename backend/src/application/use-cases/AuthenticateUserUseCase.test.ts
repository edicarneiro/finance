import { beforeEach, describe, expect, it } from "vitest";
import { AuthenticateUserUseCase } from "./AuthenticateUserUseCase";
import { RegisterUserUseCase } from "./RegisterUserUseCase";
import { SessionIssuer } from "../services/SessionIssuer";
import { InMemoryUserRepository } from "../../adapters/out/persistence/InMemoryUserRepository";
import { InMemoryRefreshTokenRepository } from "../../adapters/out/persistence/InMemoryRefreshTokenRepository";
import { InvalidCredentialsError } from "../../domain/user/errors/InvalidCredentialsError";
import { FakePasswordHasher } from "../../test-support/FakePasswordHasher";
import { SequentialIdGenerator } from "../../test-support/SequentialIdGenerator";
import { FakeTokenService } from "../../test-support/FakeTokenService";
import { SequentialRefreshTokenGenerator } from "../../test-support/SequentialRefreshTokenGenerator";
import { FixedClock } from "../../test-support/FixedClock";

const REFRESH_TOKEN_TTL_MS = 7 * 24 * 60 * 60 * 1000;

describe("AuthenticateUserUseCase", () => {
  let userRepository: InMemoryUserRepository;
  let passwordHasher: FakePasswordHasher;
  let useCase: AuthenticateUserUseCase;

  beforeEach(async () => {
    userRepository = new InMemoryUserRepository();
    passwordHasher = new FakePasswordHasher();

    const sessionIssuer = new SessionIssuer(
      new FakeTokenService(),
      new InMemoryRefreshTokenRepository(),
      new SequentialRefreshTokenGenerator(),
      new SequentialIdGenerator("session"),
      new FixedClock(new Date("2026-01-01T00:00:00.000Z")),
      REFRESH_TOKEN_TTL_MS,
    );
    useCase = new AuthenticateUserUseCase(userRepository, passwordHasher, sessionIssuer);

    const registerUser = new RegisterUserUseCase(userRepository, passwordHasher, new SequentialIdGenerator("user"));
    await registerUser.execute({ email: "user@example.com", password: "StrongPass1" });
  });

  it("issues an access token and a refresh token for valid credentials (RF-003, RF-008)", async () => {
    const result = await useCase.execute({ email: "user@example.com", password: "StrongPass1" });

    expect(result.token).toBe("token-for-user-1");
    expect(result.refreshToken).toBe("refresh-token-1");
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
});
