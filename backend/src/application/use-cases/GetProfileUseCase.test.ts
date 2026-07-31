import { beforeEach, describe, expect, it } from "vitest";
import { GetProfileUseCase } from "./GetProfileUseCase";
import { RegisterUserUseCase } from "./RegisterUserUseCase";
import { InMemoryUserRepository } from "../../adapters/out/persistence/InMemoryUserRepository";
import { UserNotFoundError } from "../../domain/user/errors/UserNotFoundError";
import { Email } from "../../domain/user/Email";
import { FakePasswordHasher } from "../../test-support/FakePasswordHasher";
import { SequentialIdGenerator } from "../../test-support/SequentialIdGenerator";

describe("GetProfileUseCase", () => {
  let userRepository: InMemoryUserRepository;
  let useCase: GetProfileUseCase;

  beforeEach(async () => {
    userRepository = new InMemoryUserRepository();
    useCase = new GetProfileUseCase(userRepository);

    const registerUser = new RegisterUserUseCase(userRepository, new FakePasswordHasher(), new SequentialIdGenerator("user"));
    await registerUser.execute({ email: "user@example.com", password: "StrongPass1" });
  });

  it("returns the profile of an existing user, with no name by default (RF-006)", async () => {
    const profile = await useCase.execute({ userId: "user-1" });

    expect(profile).toEqual({ id: "user-1", name: null, email: "user@example.com" });
  });

  it("rejects a userId that does not exist", async () => {
    await expect(useCase.execute({ userId: "ghost" })).rejects.toThrow(UserNotFoundError);
  });

  it("treats an anonymized (deleted) account as not found (RF-007)", async () => {
    const user = await userRepository.findById("user-1");
    await userRepository.update(
      user!.anonymize({
        email: Email.create("deleted-user-1@anonymized.financepulse.internal"),
        passwordHash: "unusable-hash",
        deletedAt: new Date("2026-02-01T00:00:00.000Z"),
      }),
    );

    await expect(useCase.execute({ userId: "user-1" })).rejects.toThrow(UserNotFoundError);
  });
});
