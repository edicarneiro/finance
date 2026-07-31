import { describe, expect, it } from "vitest";
import { MfaChallengeIssuer } from "./MfaChallengeIssuer";
import { InMemoryMfaChallengeRepository } from "../../adapters/out/persistence/InMemoryMfaChallengeRepository";
import { SequentialMfaChallengeGenerator } from "../../test-support/SequentialMfaChallengeGenerator";
import { SequentialIdGenerator } from "../../test-support/SequentialIdGenerator";
import { FixedClock } from "../../test-support/FixedClock";

const NOW = new Date("2026-01-01T00:00:00.000Z");
const CHALLENGE_TTL_MS = 5 * 60 * 1000;

describe("MfaChallengeIssuer", () => {
  it("issues and persists a challenge for a user, expiring after the configured TTL", async () => {
    const repository = new InMemoryMfaChallengeRepository();
    const issuer = new MfaChallengeIssuer(
      repository,
      new SequentialMfaChallengeGenerator(),
      new SequentialIdGenerator("challenge"),
      new FixedClock(NOW),
      CHALLENGE_TTL_MS,
    );

    const result = await issuer.issueFor("user-1");

    expect(result.challengeToken).toBe("mfa-challenge-1");
    const stored = await repository.findByRawToken("mfa-challenge-1");
    expect(stored?.userId).toBe("user-1");
    expect(stored?.isValid(NOW)).toBe(true);
    expect(stored?.expiresAt.getTime()).toBe(NOW.getTime() + CHALLENGE_TTL_MS);
  });

  it("issues a different challenge token on each call", async () => {
    const repository = new InMemoryMfaChallengeRepository();
    const issuer = new MfaChallengeIssuer(
      repository,
      new SequentialMfaChallengeGenerator(),
      new SequentialIdGenerator("challenge"),
      new FixedClock(NOW),
      CHALLENGE_TTL_MS,
    );

    const first = await issuer.issueFor("user-1");
    const second = await issuer.issueFor("user-1");

    expect(first.challengeToken).not.toBe(second.challengeToken);
  });
});
