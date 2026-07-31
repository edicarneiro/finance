import { beforeEach, describe, expect, it } from "vitest";
import { ConfirmMfaEnrollmentUseCase } from "./ConfirmMfaEnrollmentUseCase";
import { EnrollMfaUseCase } from "./EnrollMfaUseCase";
import { RegisterUserUseCase } from "./RegisterUserUseCase";
import { InMemoryUserRepository } from "../../adapters/out/persistence/InMemoryUserRepository";
import { InMemoryMfaCredentialRepository } from "../../adapters/out/persistence/InMemoryMfaCredentialRepository";
import { InvalidMfaCodeError } from "../../domain/user/errors/InvalidMfaCodeError";
import { MfaNotEnrolledError } from "../../domain/user/errors/MfaNotEnrolledError";
import { UserNotFoundError } from "../../domain/user/errors/UserNotFoundError";
import { FakePasswordHasher } from "../../test-support/FakePasswordHasher";
import { SequentialIdGenerator } from "../../test-support/SequentialIdGenerator";
import { FakeTotpService } from "../../test-support/FakeTotpService";
import { FixedClock } from "../../test-support/FixedClock";

const NOW = new Date("2026-01-01T00:00:00.000Z");

describe("ConfirmMfaEnrollmentUseCase", () => {
  let userRepository: InMemoryUserRepository;
  let mfaCredentialRepository: InMemoryMfaCredentialRepository;
  let useCase: ConfirmMfaEnrollmentUseCase;

  beforeEach(async () => {
    userRepository = new InMemoryUserRepository();
    mfaCredentialRepository = new InMemoryMfaCredentialRepository();
    useCase = new ConfirmMfaEnrollmentUseCase(userRepository, mfaCredentialRepository, new FakeTotpService(), new FixedClock(NOW));

    const registerUser = new RegisterUserUseCase(userRepository, new FakePasswordHasher(), new SequentialIdGenerator("user"));
    await registerUser.execute({ email: "user@example.com", password: "StrongPass1" });
    await registerUser.execute({ email: "other@example.com", password: "StrongPass1" });

    const enroll = new EnrollMfaUseCase(
      userRepository,
      new FakePasswordHasher(),
      mfaCredentialRepository,
      new FakeTotpService(),
      new SequentialIdGenerator("mfa"),
      new FixedClock(NOW),
    );
    await enroll.execute({ userId: "user-1", password: "StrongPass1" });
  });

  it("activates the pending credential when the code is valid (RF-004)", async () => {
    await useCase.execute({ userId: "user-1", code: "valid-code-for-FAKE_SECRET" });

    const credential = await mfaCredentialRepository.findByUserId("user-1");
    expect(credential?.isActive()).toBe(true);
  });

  it("rejects an invalid code, leaving the credential unconfirmed", async () => {
    await expect(useCase.execute({ userId: "user-1", code: "000000" })).rejects.toThrow(InvalidMfaCodeError);

    const credential = await mfaCredentialRepository.findByUserId("user-1");
    expect(credential?.isConfirmed()).toBe(false);
  });

  it("rejects confirmation for an existing user with no pending enrollment", async () => {
    await expect(useCase.execute({ userId: "user-2", code: "valid-code-for-FAKE_SECRET" })).rejects.toThrow(
      MfaNotEnrolledError,
    );
  });

  it("rejects confirming a credential that is already confirmed", async () => {
    await useCase.execute({ userId: "user-1", code: "valid-code-for-FAKE_SECRET" });

    await expect(useCase.execute({ userId: "user-1", code: "valid-code-for-FAKE_SECRET" })).rejects.toThrow(
      MfaNotEnrolledError,
    );
  });

  it("rejects confirmation for a userId that has no user account at all", async () => {
    await expect(useCase.execute({ userId: "ghost", code: "valid-code-for-FAKE_SECRET" })).rejects.toThrow(
      UserNotFoundError,
    );
  });
});
