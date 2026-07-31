import { beforeEach, describe, expect, it } from "vitest";
import { RecordConsentUseCase } from "./RecordConsentUseCase";
import { RegisterUserUseCase } from "./RegisterUserUseCase";
import { InMemoryUserRepository } from "../../adapters/out/persistence/InMemoryUserRepository";
import { InMemoryConsentRepository } from "../../adapters/out/persistence/InMemoryConsentRepository";
import { UserNotFoundError } from "../../domain/user/errors/UserNotFoundError";
import { InvalidConsentVersionError } from "../../domain/user/errors/InvalidConsentVersionError";
import { FakePasswordHasher } from "../../test-support/FakePasswordHasher";
import { SequentialIdGenerator } from "../../test-support/SequentialIdGenerator";
import { FixedClock } from "../../test-support/FixedClock";

const ACCEPTED_AT = new Date("2026-01-01T00:00:00.000Z");

describe("RecordConsentUseCase", () => {
  let userRepository: InMemoryUserRepository;
  let consentRepository: InMemoryConsentRepository;
  let useCase: RecordConsentUseCase;

  beforeEach(async () => {
    userRepository = new InMemoryUserRepository();
    consentRepository = new InMemoryConsentRepository();
    useCase = new RecordConsentUseCase(
      userRepository,
      consentRepository,
      new SequentialIdGenerator("consent"),
      new FixedClock(ACCEPTED_AT),
    );

    const registerUser = new RegisterUserUseCase(userRepository, new FakePasswordHasher(), new SequentialIdGenerator("user"));
    await registerUser.execute({ email: "user@example.com", password: "StrongPass1" });
  });

  it("records a new consent for an existing user (RF-046)", async () => {
    const result = await useCase.execute({ userId: "user-1", version: "privacy-policy-v1" });

    expect(result).toEqual({ id: "consent-1", version: "privacy-policy-v1", acceptedAt: ACCEPTED_AT });

    const history = await consentRepository.findAllForUser("user-1");
    expect(history).toHaveLength(1);
  });

  it("allows recording multiple consents over time without overwriting previous ones", async () => {
    await useCase.execute({ userId: "user-1", version: "privacy-policy-v1" });
    await useCase.execute({ userId: "user-1", version: "privacy-policy-v2" });

    const history = await consentRepository.findAllForUser("user-1");
    expect(history).toHaveLength(2);
  });

  it("rejects an empty version", async () => {
    await expect(useCase.execute({ userId: "user-1", version: "" })).rejects.toThrow(InvalidConsentVersionError);
  });

  it("rejects a userId that does not exist", async () => {
    await expect(useCase.execute({ userId: "ghost", version: "privacy-policy-v1" })).rejects.toThrow(
      UserNotFoundError,
    );
  });
});
