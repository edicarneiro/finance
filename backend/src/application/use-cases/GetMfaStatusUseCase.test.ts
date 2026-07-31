import { beforeEach, describe, expect, it } from "vitest";
import { GetMfaStatusUseCase } from "./GetMfaStatusUseCase";
import { EnrollMfaUseCase } from "./EnrollMfaUseCase";
import { ConfirmMfaEnrollmentUseCase } from "./ConfirmMfaEnrollmentUseCase";
import { RegisterUserUseCase } from "./RegisterUserUseCase";
import { InMemoryUserRepository } from "../../adapters/out/persistence/InMemoryUserRepository";
import { InMemoryMfaCredentialRepository } from "../../adapters/out/persistence/InMemoryMfaCredentialRepository";
import { UserNotFoundError } from "../../domain/user/errors/UserNotFoundError";
import { FakePasswordHasher } from "../../test-support/FakePasswordHasher";
import { SequentialIdGenerator } from "../../test-support/SequentialIdGenerator";
import { FakeTotpService } from "../../test-support/FakeTotpService";
import { FixedClock } from "../../test-support/FixedClock";

const NOW = new Date("2026-01-01T00:00:00.000Z");

describe("GetMfaStatusUseCase", () => {
  let userRepository: InMemoryUserRepository;
  let mfaCredentialRepository: InMemoryMfaCredentialRepository;
  let useCase: GetMfaStatusUseCase;

  beforeEach(async () => {
    userRepository = new InMemoryUserRepository();
    mfaCredentialRepository = new InMemoryMfaCredentialRepository();
    useCase = new GetMfaStatusUseCase(userRepository, mfaCredentialRepository);

    const registerUser = new RegisterUserUseCase(userRepository, new FakePasswordHasher(), new SequentialIdGenerator("user"));
    await registerUser.execute({ email: "user@example.com", password: "StrongPass1" });
  });

  it("reports disabled when the user never enrolled", async () => {
    expect(await useCase.execute({ userId: "user-1" })).toEqual({ enabled: false });
  });

  it("reports disabled while enrollment is pending confirmation", async () => {
    const enroll = new EnrollMfaUseCase(
      userRepository,
      new FakePasswordHasher(),
      mfaCredentialRepository,
      new FakeTotpService(),
      new SequentialIdGenerator("mfa"),
      new FixedClock(NOW),
    );
    await enroll.execute({ userId: "user-1", password: "StrongPass1" });

    expect(await useCase.execute({ userId: "user-1" })).toEqual({ enabled: false });
  });

  it("reports enabled once confirmed", async () => {
    const enroll = new EnrollMfaUseCase(
      userRepository,
      new FakePasswordHasher(),
      mfaCredentialRepository,
      new FakeTotpService(),
      new SequentialIdGenerator("mfa"),
      new FixedClock(NOW),
    );
    await enroll.execute({ userId: "user-1", password: "StrongPass1" });
    const confirm = new ConfirmMfaEnrollmentUseCase(userRepository, mfaCredentialRepository, new FakeTotpService(), new FixedClock(NOW));
    await confirm.execute({ userId: "user-1", code: "valid-code-for-FAKE_SECRET" });

    expect(await useCase.execute({ userId: "user-1" })).toEqual({ enabled: true });
  });

  it("rejects a userId that does not exist", async () => {
    await expect(useCase.execute({ userId: "ghost" })).rejects.toThrow(UserNotFoundError);
  });
});
