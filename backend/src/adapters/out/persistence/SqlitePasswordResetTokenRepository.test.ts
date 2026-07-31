import { afterEach, beforeEach, describe, expect, it } from "vitest";
import Database from "better-sqlite3";
import { SqlitePasswordResetTokenRepository } from "./SqlitePasswordResetTokenRepository";
import { PasswordResetToken } from "../../../domain/user/PasswordResetToken";

const ISSUED_AT = new Date("2026-01-01T00:00:00.000Z");
const EXPIRES_AT = new Date("2026-01-01T01:00:00.000Z");

describe("SqlitePasswordResetTokenRepository", () => {
  let database: Database.Database;
  let repository: SqlitePasswordResetTokenRepository;

  beforeEach(() => {
    database = new Database(":memory:");
    repository = new SqlitePasswordResetTokenRepository(database);
  });

  afterEach(() => {
    database.close();
  });

  it("returns null for a raw token that was never stored", async () => {
    expect(await repository.findByRawToken("never-issued")).toBeNull();
  });

  it("persists a token by its hash and retrieves it by the raw value", async () => {
    const token = PasswordResetToken.issue({ id: "reset-1", userId: "user-1", expiresAt: EXPIRES_AT, createdAt: ISSUED_AT });
    await repository.save(token, "raw-token-value");

    const found = await repository.findByRawToken("raw-token-value");
    expect(found?.id).toBe("reset-1");
    expect(found?.userId).toBe("user-1");
    expect(found?.isUsed()).toBe(false);
  });

  it("never stores the raw token value as plain text", async () => {
    const token = PasswordResetToken.issue({ id: "reset-1", userId: "user-1", expiresAt: EXPIRES_AT, createdAt: ISSUED_AT });
    await repository.save(token, "raw-token-value");

    const row = database.prepare("SELECT token_hash FROM password_reset_tokens WHERE id = ?").get("reset-1") as {
      token_hash: string;
    };
    expect(row.token_hash).not.toBe("raw-token-value");
  });

  it("marks a token as used by id", async () => {
    const token = PasswordResetToken.issue({ id: "reset-1", userId: "user-1", expiresAt: EXPIRES_AT, createdAt: ISSUED_AT });
    await repository.save(token, "raw-token-value");

    await repository.markUsed("reset-1", ISSUED_AT);

    expect((await repository.findByRawToken("raw-token-value"))?.isUsed()).toBe(true);
  });

  it("invalidates all active tokens for a user, leaving other users untouched", async () => {
    const tokenA = PasswordResetToken.issue({ id: "reset-a", userId: "user-1", expiresAt: EXPIRES_AT, createdAt: ISSUED_AT });
    const tokenB = PasswordResetToken.issue({ id: "reset-b", userId: "user-2", expiresAt: EXPIRES_AT, createdAt: ISSUED_AT });
    await repository.save(tokenA, "raw-a");
    await repository.save(tokenB, "raw-b");

    await repository.invalidateAllForUser("user-1", ISSUED_AT);

    expect((await repository.findByRawToken("raw-a"))?.isUsed()).toBe(true);
    expect((await repository.findByRawToken("raw-b"))?.isUsed()).toBe(false);
  });
});
