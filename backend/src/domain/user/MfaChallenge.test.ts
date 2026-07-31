import { describe, expect, it } from "vitest";
import { MfaChallenge } from "./MfaChallenge";

const ISSUED_AT = new Date("2026-01-01T00:00:00.000Z");
const EXPIRES_AT = new Date("2026-01-01T00:05:00.000Z");

describe("MfaChallenge", () => {
  it("is valid right after being issued", () => {
    const challenge = MfaChallenge.issue({ id: "challenge-1", userId: "user-1", expiresAt: EXPIRES_AT, createdAt: ISSUED_AT });

    expect(challenge.isValid(ISSUED_AT)).toBe(true);
    expect(challenge.isUsed()).toBe(false);
    expect(challenge.isExpired(ISSUED_AT)).toBe(false);
  });

  it("is expired once the current time reaches expiresAt", () => {
    const challenge = MfaChallenge.issue({ id: "challenge-1", userId: "user-1", expiresAt: EXPIRES_AT, createdAt: ISSUED_AT });

    expect(challenge.isExpired(EXPIRES_AT)).toBe(true);
    expect(challenge.isValid(EXPIRES_AT)).toBe(false);
  });

  it("becomes invalid once used, even before expiring", () => {
    const challenge = MfaChallenge.issue({ id: "challenge-1", userId: "user-1", expiresAt: EXPIRES_AT, createdAt: ISSUED_AT });

    challenge.markUsed(ISSUED_AT);

    expect(challenge.isUsed()).toBe(true);
    expect(challenge.isValid(ISSUED_AT)).toBe(false);
  });

  it("restores a previously used challenge from persisted data", () => {
    const usedAt = new Date("2026-01-01T00:01:00.000Z");

    const challenge = MfaChallenge.restore({ id: "challenge-1", userId: "user-1", expiresAt: EXPIRES_AT, createdAt: ISSUED_AT, usedAt });

    expect(challenge.isUsed()).toBe(true);
  });
});
