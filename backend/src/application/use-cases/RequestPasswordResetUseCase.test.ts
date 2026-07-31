import { beforeEach, describe, expect, it } from "vitest";
import { RequestPasswordResetUseCase } from "./RequestPasswordResetUseCase";
import { RegisterUserUseCase } from "./RegisterUserUseCase";
import { InMemoryUserRepository } from "../../adapters/out/persistence/InMemoryUserRepository";
import { InMemoryPasswordResetTokenRepository } from "../../adapters/out/persistence/InMemoryPasswordResetTokenRepository";
import { InvalidEmailError } from "../../domain/user/errors/InvalidEmailError";
import { FakePasswordHasher } from "../../test-support/FakePasswordHasher";
import { SequentialIdGenerator } from "../../test-support/SequentialIdGenerator";
import { SequentialPasswordResetTokenGenerator } from "../../test-support/SequentialPasswordResetTokenGenerator";
import { FakePasswordResetNotifier } from "../../test-support/FakePasswordResetNotifier";
import { FixedClock } from "../../test-support/FixedClock";

const NOW = new Date("2026-01-01T00:00:00.000Z");
const TTL_MS = 60 * 60 * 1000;

describe("RequestPasswordResetUseCase", () => {
  let userRepository: InMemoryUserRepository;
  let tokenRepository: InMemoryPasswordResetTokenRepository;
  let notifier: FakePasswordResetNotifier;
  let useCase: RequestPasswordResetUseCase;

  beforeEach(async () => {
    userRepository = new InMemoryUserRepository();
    tokenRepository = new InMemoryPasswordResetTokenRepository();
    notifier = new FakePasswordResetNotifier();
    useCase = new RequestPasswordResetUseCase(
      userRepository,
      tokenRepository,
      new SequentialPasswordResetTokenGenerator(),
      new SequentialIdGenerator("reset"),
      new FixedClock(NOW),
      notifier,
      TTL_MS,
    );

    const registerUser = new RegisterUserUseCase(userRepository, new FakePasswordHasher(), new SequentialIdGenerator("user"));
    await registerUser.execute({ email: "user@example.com", password: "StrongPass1" });
  });

  it("issues a reset token and notifies the user when the email exists (RF-005)", async () => {
    await useCase.execute({ email: "user@example.com" });

    expect(notifier.sent).toHaveLength(1);
    expect(notifier.sent[0].email.toString()).toBe("user@example.com");
    expect(notifier.sent[0].rawToken).toBe("reset-token-1");
    expect(notifier.sent[0].expiresAt.getTime()).toBe(NOW.getTime() + TTL_MS);

    const stored = await tokenRepository.findByRawToken("reset-token-1");
    expect(stored?.isValid(NOW)).toBe(true);
  });

  it("does not notify and does not throw when the email does not exist (anti-enumeration)", async () => {
    await expect(useCase.execute({ email: "ghost@example.com" })).resolves.toBeUndefined();

    expect(notifier.sent).toHaveLength(0);
  });

  it("still rejects a malformed email (format, not existence)", async () => {
    await expect(useCase.execute({ email: "not-an-email" })).rejects.toThrow(InvalidEmailError);
  });

  it("invalidates a previous outstanding reset token when a new one is requested", async () => {
    await useCase.execute({ email: "user@example.com" });
    const firstToken = notifier.sent[0].rawToken;

    await useCase.execute({ email: "user@example.com" });

    const first = await tokenRepository.findByRawToken(firstToken);
    expect(first?.isValid(NOW)).toBe(false);
  });
});
