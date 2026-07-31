import { beforeEach, describe, expect, it } from "vitest";
import { UpdateProfileUseCase } from "./UpdateProfileUseCase";
import { RegisterUserUseCase } from "./RegisterUserUseCase";
import { InMemoryUserRepository } from "../../adapters/out/persistence/InMemoryUserRepository";
import { UserNotFoundError } from "../../domain/user/errors/UserNotFoundError";
import { DuplicateEmailError } from "../../domain/user/errors/DuplicateEmailError";
import { InvalidNameError } from "../../domain/user/errors/InvalidNameError";
import { Email } from "../../domain/user/Email";
import { FakePasswordHasher } from "../../test-support/FakePasswordHasher";
import { SequentialIdGenerator } from "../../test-support/SequentialIdGenerator";

describe("UpdateProfileUseCase", () => {
  let userRepository: InMemoryUserRepository;
  let useCase: UpdateProfileUseCase;

  beforeEach(async () => {
    userRepository = new InMemoryUserRepository();
    useCase = new UpdateProfileUseCase(userRepository);

    const registerUser = new RegisterUserUseCase(userRepository, new FakePasswordHasher(), new SequentialIdGenerator("user"));
    await registerUser.execute({ email: "user@example.com", password: "StrongPass1" });
    await registerUser.execute({ email: "other@example.com", password: "StrongPass1" });
  });

  it("updates the name and email of an existing user (RF-006)", async () => {
    const result = await useCase.execute({ userId: "user-1", name: "Ana Souza", email: "ana@example.com" });

    expect(result).toEqual({ id: "user-1", name: "Ana Souza", email: "ana@example.com" });
  });

  it("allows keeping the same email while changing only the name", async () => {
    const result = await useCase.execute({ userId: "user-1", name: "Ana Souza", email: "user@example.com" });

    expect(result.email).toBe("user@example.com");
  });

  it("rejects updating to an email already used by another user (RF-002)", async () => {
    await expect(
      useCase.execute({ userId: "user-1", name: "Ana Souza", email: "other@example.com" }),
    ).rejects.toThrow(DuplicateEmailError);
  });

  it("rejects an empty name", async () => {
    await expect(useCase.execute({ userId: "user-1", name: "", email: "user@example.com" })).rejects.toThrow(
      InvalidNameError,
    );
  });

  it("rejects a userId that does not exist", async () => {
    await expect(useCase.execute({ userId: "ghost", name: "Ana Souza", email: "ana@example.com" })).rejects.toThrow(
      UserNotFoundError,
    );
  });

  it("rejects editing an anonymized (deleted) account, e.g. via a still-valid access token (RF-007)", async () => {
    const user = await userRepository.findById("user-1");
    await userRepository.update(
      user!.anonymize({
        email: Email.create("deleted-user-1@anonymized.financepulse.internal"),
        passwordHash: "unusable-hash",
        deletedAt: new Date("2026-02-01T00:00:00.000Z"),
      }),
    );

    await expect(
      useCase.execute({ userId: "user-1", name: "Ana Souza", email: "ana@example.com" }),
    ).rejects.toThrow(UserNotFoundError);
  });
});
