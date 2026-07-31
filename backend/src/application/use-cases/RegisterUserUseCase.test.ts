import { beforeEach, describe, expect, it } from "vitest";
import { RegisterUserUseCase } from "./RegisterUserUseCase";
import { InMemoryUserRepository } from "../../adapters/out/persistence/InMemoryUserRepository";
import { Email } from "../../domain/user/Email";
import { DuplicateEmailError } from "../../domain/user/errors/DuplicateEmailError";
import { WeakPasswordError } from "../../domain/user/errors/WeakPasswordError";
import type { PasswordHasher } from "../ports/PasswordHasher";
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

describe("RegisterUserUseCase", () => {
  let userRepository: InMemoryUserRepository;
  let useCase: RegisterUserUseCase;

  beforeEach(() => {
    userRepository = new InMemoryUserRepository();
    useCase = new RegisterUserUseCase(userRepository, new FakePasswordHasher(), new SequentialIdGenerator());
  });

  it("registers a new user and persists a hashed password (RF-001)", async () => {
    const result = await useCase.execute({ email: "user@example.com", password: "StrongPass1" });

    expect(result.userId).toBe("user-1");

    const saved = await userRepository.findByEmail(Email.create("user@example.com"));
    expect(saved?.passwordHash).toBe("hashed:StrongPass1");
    expect(saved?.passwordHash).not.toBe("StrongPass1");
  });

  it("rejects registration with an email already in use, case-insensitively (RF-002)", async () => {
    await useCase.execute({ email: "user@example.com", password: "StrongPass1" });

    await expect(useCase.execute({ email: "USER@example.com", password: "AnotherPass1" })).rejects.toThrow(
      DuplicateEmailError,
    );
  });

  it("rejects a password shorter than the minimum length", async () => {
    await expect(useCase.execute({ email: "user@example.com", password: "short" })).rejects.toThrow(
      WeakPasswordError,
    );
  });

  it("does not persist a user when the password is weak", async () => {
    await expect(useCase.execute({ email: "user@example.com", password: "short" })).rejects.toThrow();

    const saved = await userRepository.findByEmail(Email.create("user@example.com"));
    expect(saved).toBeNull();
  });
});
