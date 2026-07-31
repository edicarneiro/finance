import { describe, expect, it } from "vitest";
import { PasswordResetToken } from "./PasswordResetToken";

const ISSUED_AT = new Date("2026-01-01T00:00:00.000Z");
const EXPIRES_AT = new Date("2026-01-01T01:00:00.000Z");

describe("PasswordResetToken", () => {
  it("is valid right after being issued", () => {
    const token = PasswordResetToken.issue({ id: "reset-1", userId: "user-1", expiresAt: EXPIRES_AT, createdAt: ISSUED_AT });

    expect(token.isValid(ISSUED_AT)).toBe(true);
    expect(token.isUsed()).toBe(false);
    expect(token.isExpired(ISSUED_AT)).toBe(false);
  });

  it("is expired once the current time reaches expiresAt", () => {
    const token = PasswordResetToken.issue({ id: "reset-1", userId: "user-1", expiresAt: EXPIRES_AT, createdAt: ISSUED_AT });

    expect(token.isExpired(EXPIRES_AT)).toBe(true);
    expect(token.isValid(EXPIRES_AT)).toBe(false);
  });

  it("becomes invalid once used, even before expiring", () => {
    const token = PasswordResetToken.issue({ id: "reset-1", userId: "user-1", expiresAt: EXPIRES_AT, createdAt: ISSUED_AT });

    token.markUsed(ISSUED_AT);

    expect(token.isUsed()).toBe(true);
    expect(token.isValid(ISSUED_AT)).toBe(false);
  });

  it("restores a previously used token from persisted data", () => {
    const usedAt = new Date("2026-01-01T00:30:00.000Z");

    const token = PasswordResetToken.restore({ id: "reset-1", userId: "user-1", expiresAt: EXPIRES_AT, createdAt: ISSUED_AT, usedAt });

    expect(token.isUsed()).toBe(true);
  });
});
