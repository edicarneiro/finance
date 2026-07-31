import { beforeEach, describe, expect, it } from "vitest";
import { AuthenticateUserUseCase } from "./AuthenticateUserUseCase";
import { RegisterUserUseCase } from "./RegisterUserUseCase";
import { InMemoryUserRepository } from "../../adapters/out/persistence/InMemoryUserRepository";
import { InvalidCredentialsError } from "../../domain/user/errors/InvalidCredentialsError";
import type { PasswordHasher } from "../ports/PasswordHasher";
import type { TokenService, TokenPayload } from "../ports/TokenService";
import type { IdGenerator } from "../ports/IdGenerator";

class FakePasswordHasher implements PasswordHasher {
  async hash(plainPassword: string): Promise<string> {
    return `hashed:${plainPassword}`;
  }

  async compare(plainPassword: string, passwordHash: string): Promise<boolean> {
    return passwordHash === `hashed:${plainPassword}`;
  }
}

class SequentialIdGenerator implements IdGenerator {
  private counter = 0;

  generate(): string {
    this.counter += 1;
    return `user-${this.counter}`;
  }
}

class FakeTokenService implements TokenService {
  issue(userId: string): string {
    return `token-for-${userId}`;
  }

  verify(token: string): TokenPayload | null {
    const match = /^token-for-(.+)$/.exec(token);
    return match ? { userId: match[1] } : null;
  }
}

describe("AuthenticateUserUseCase", () => {
  let userRepository: InMemoryUserRepository;
  let passwordHasher: FakePasswordHasher;
  let tokenService: FakeTokenService;
  let useCase: AuthenticateUserUseCase;

  beforeEach(async () => {
    userRepository = new InMemoryUserRepository();
    passwordHasher = new FakePasswordHasher();
    tokenService = new FakeTokenService();
    useCase = new AuthenticateUserUseCase(userRepository, passwordHasher, tokenService);

    const registerUser = new RegisterUserUseCase(userRepository, passwordHasher, new SequentialIdGenerator());
    await registerUser.execute({ email: "user@example.com", password: "StrongPass1" });
  });

  it("issues a session token for valid credentials (RF-003, RF-008)", async () => {
    const result = await useCase.execute({ email: "user@example.com", password: "StrongPass1" });

    expect(result.token).toBe("token-for-user-1");
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
