import { beforeEach, describe, expect, it } from "vitest";
import { GetProfileUseCase } from "./GetProfileUseCase";
import { RegisterUserUseCase } from "./RegisterUserUseCase";
import { InMemoryUserRepository } from "../../adapters/out/persistence/InMemoryUserRepository";
import { UserNotFoundError } from "../../domain/user/errors/UserNotFoundError";
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
});
