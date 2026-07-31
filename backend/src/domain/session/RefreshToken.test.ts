import { describe, expect, it } from "vitest";
import { RefreshToken } from "./RefreshToken";

const ISSUED_AT = new Date("2026-01-01T00:00:00.000Z");
const EXPIRES_AT = new Date("2026-01-08T00:00:00.000Z");

describe("RefreshToken", () => {
  it("is valid right after being issued", () => {
    const token = RefreshToken.issue({
      id: "token-1",
      userId: "user-1",
      expiresAt: EXPIRES_AT,
      createdAt: ISSUED_AT,
    });

    expect(token.isValid(ISSUED_AT)).toBe(true);
    expect(token.isRevoked()).toBe(false);
    expect(token.isExpired(ISSUED_AT)).toBe(false);
  });

  it("is expired once the current time reaches expiresAt", () => {
    const token = RefreshToken.issue({
      id: "token-1",
      userId: "user-1",
      expiresAt: EXPIRES_AT,
      createdAt: ISSUED_AT,
    });

    expect(token.isExpired(EXPIRES_AT)).toBe(true);
    expect(token.isValid(EXPIRES_AT)).toBe(false);
  });

  it("becomes invalid once revoked, even before expiring", () => {
    const token = RefreshToken.issue({
      id: "token-1",
      userId: "user-1",
      expiresAt: EXPIRES_AT,
      createdAt: ISSUED_AT,
    });

    token.revoke(ISSUED_AT);

    expect(token.isRevoked()).toBe(true);
    expect(token.isValid(ISSUED_AT)).toBe(false);
  });

  it("restores a previously revoked token from persisted data", () => {
    const revokedAt = new Date("2026-01-02T00:00:00.000Z");

    const token = RefreshToken.restore({
      id: "token-1",
      userId: "user-1",
      expiresAt: EXPIRES_AT,
      createdAt: ISSUED_AT,
      revokedAt,
    });

    expect(token.isRevoked()).toBe(true);
  });
});
