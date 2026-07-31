import { beforeEach, describe, expect, it } from "vitest";
import { DisableMfaUseCase } from "./DisableMfaUseCase";
import { EnrollMfaUseCase } from "./EnrollMfaUseCase";
import { ConfirmMfaEnrollmentUseCase } from "./ConfirmMfaEnrollmentUseCase";
import { RegisterUserUseCase } from "./RegisterUserUseCase";
import { InMemoryUserRepository } from "../../adapters/out/persistence/InMemoryUserRepository";
import { InMemoryMfaCredentialRepository } from "../../adapters/out/persistence/InMemoryMfaCredentialRepository";
import { InvalidCredentialsError } from "../../domain/user/errors/InvalidCredentialsError";
import { MfaNotEnrolledError } from "../../domain/user/errors/MfaNotEnrolledError";
import { FakePasswordHasher } from "../../test-support/FakePasswordHasher";
import { SequentialIdGenerator } from "../../test-support/SequentialIdGenerator";
import { FakeTotpService } from "../../test-support/FakeTotpService";
import { FixedClock } from "../../test-support/FixedClock";

const NOW = new Date("2026-01-01T00:00:00.000Z");

describe("DisableMfaUseCase", () => {
  let userRepository: InMemoryUserRepository;
  let mfaCredentialRepository: InMemoryMfaCredentialRepository;
  let useCase: DisableMfaUseCase;

  beforeEach(async () => {
    userRepository = new InMemoryUserRepository();
    mfaCredentialRepository = new InMemoryMfaCredentialRepository();
    useCase = new DisableMfaUseCase(userRepository, new FakePasswordHasher(), mfaCredentialRepository, new FixedClock(NOW));

    const registerUser = new RegisterUserUseCase(userRepository, new FakePasswordHasher(), new SequentialIdGenerator("user"));
    await registerUser.execute({ email: "user@example.com", password: "StrongPass1" });

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
  });

  it("disables an active credential when the password is correct (RF-004)", async () => {
    await useCase.execute({ userId: "user-1", password: "StrongPass1" });

    const credential = await mfaCredentialRepository.findByUserId("user-1");
    expect(credential?.isActive()).toBe(false);
    expect(credential?.isDisabled()).toBe(true);
  });

  it("rejects disabling with an incorrect confirmation password, leaving MFA active", async () => {
    await expect(useCase.execute({ userId: "user-1", password: "WrongPass1" })).rejects.toThrow(
      InvalidCredentialsError,
    );

    const credential = await mfaCredentialRepository.findByUserId("user-1");
    expect(credential?.isActive()).toBe(true);
  });

  it("rejects disabling when there is no active MFA credential", async () => {
    await useCase.execute({ userId: "user-1", password: "StrongPass1" });

    await expect(useCase.execute({ userId: "user-1", password: "StrongPass1" })).rejects.toThrow(
      MfaNotEnrolledError,
    );
  });
});
