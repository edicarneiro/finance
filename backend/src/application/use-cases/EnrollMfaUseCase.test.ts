import { beforeEach, describe, expect, it } from "vitest";
import { EnrollMfaUseCase } from "./EnrollMfaUseCase";
import { RegisterUserUseCase } from "./RegisterUserUseCase";
import { InMemoryUserRepository } from "../../adapters/out/persistence/InMemoryUserRepository";
import { InMemoryMfaCredentialRepository } from "../../adapters/out/persistence/InMemoryMfaCredentialRepository";
import { InvalidCredentialsError } from "../../domain/user/errors/InvalidCredentialsError";
import { UserNotFoundError } from "../../domain/user/errors/UserNotFoundError";
import { FakePasswordHasher } from "../../test-support/FakePasswordHasher";
import { SequentialIdGenerator } from "../../test-support/SequentialIdGenerator";
import { FakeTotpService } from "../../test-support/FakeTotpService";
import { FixedClock } from "../../test-support/FixedClock";

const NOW = new Date("2026-01-01T00:00:00.000Z");

describe("EnrollMfaUseCase", () => {
  let userRepository: InMemoryUserRepository;
  let mfaCredentialRepository: InMemoryMfaCredentialRepository;
  let useCase: EnrollMfaUseCase;

  beforeEach(async () => {
    userRepository = new InMemoryUserRepository();
    mfaCredentialRepository = new InMemoryMfaCredentialRepository();
    useCase = new EnrollMfaUseCase(
      userRepository,
      new FakePasswordHasher(),
      mfaCredentialRepository,
      new FakeTotpService(),
      new SequentialIdGenerator("mfa"),
      new FixedClock(NOW),
    );

    const registerUser = new RegisterUserUseCase(userRepository, new FakePasswordHasher(), new SequentialIdGenerator("user"));
    await registerUser.execute({ email: "user@example.com", password: "StrongPass1" });
  });

  it("generates and persists a pending (unconfirmed) credential when the password is correct (RF-004)", async () => {
    const result = await useCase.execute({ userId: "user-1", password: "StrongPass1" });

    expect(result.secret).toBe("FAKE_SECRET");
    expect(result.otpauthUrl).toContain("FAKE_SECRET");

    const credential = await mfaCredentialRepository.findByUserId("user-1");
    expect(credential?.isConfirmed()).toBe(false);
    expect(credential?.isActive()).toBe(false);
  });

  it("rejects enrollment with an incorrect confirmation password", async () => {
    await expect(useCase.execute({ userId: "user-1", password: "WrongPass1" })).rejects.toThrow(
      InvalidCredentialsError,
    );

    expect(await mfaCredentialRepository.findByUserId("user-1")).toBeNull();
  });

  it("rejects a userId that does not exist", async () => {
    await expect(useCase.execute({ userId: "ghost", password: "StrongPass1" })).rejects.toThrow(UserNotFoundError);
  });

  it("replaces any prior credential (confirmed or not) with a fresh one", async () => {
    await useCase.execute({ userId: "user-1", password: "StrongPass1" });
    const first = await mfaCredentialRepository.findByUserId("user-1");
    await mfaCredentialRepository.confirm(first!.id, NOW);

    await useCase.execute({ userId: "user-1", password: "StrongPass1" });

    const second = await mfaCredentialRepository.findByUserId("user-1");
    expect(second?.id).toBe("mfa-2");
    expect(second?.isConfirmed()).toBe(false);
  });
});
