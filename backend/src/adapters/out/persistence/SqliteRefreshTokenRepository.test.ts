import { afterEach, beforeEach, describe, expect, it } from "vitest";
import Database from "better-sqlite3";
import { SqliteRefreshTokenRepository } from "./SqliteRefreshTokenRepository";
import { RefreshToken } from "../../../domain/session/RefreshToken";

const ISSUED_AT = new Date("2026-01-01T00:00:00.000Z");
const EXPIRES_AT = new Date("2026-01-08T00:00:00.000Z");

describe("SqliteRefreshTokenRepository", () => {
  let database: Database.Database;
  let repository: SqliteRefreshTokenRepository;

  beforeEach(() => {
    database = new Database(":memory:");
    repository = new SqliteRefreshTokenRepository(database);
  });

  afterEach(() => {
    database.close();
  });

  it("returns null for a raw token that was never stored", async () => {
    const found = await repository.findByRawToken("never-issued");

    expect(found).toBeNull();
  });

  it("persists a token by its hash and retrieves it by the raw value", async () => {
    const refreshToken = RefreshToken.issue({
      id: "token-1",
      userId: "user-1",
      expiresAt: EXPIRES_AT,
      createdAt: ISSUED_AT,
    });

    await repository.save(refreshToken, "raw-token-value");

    const found = await repository.findByRawToken("raw-token-value");
    expect(found?.id).toBe("token-1");
    expect(found?.userId).toBe("user-1");
    expect(found?.isRevoked()).toBe(false);
  });

  it("never stores the raw token value as plain text", async () => {
    const refreshToken = RefreshToken.issue({
      id: "token-1",
      userId: "user-1",
      expiresAt: EXPIRES_AT,
      createdAt: ISSUED_AT,
    });

    await repository.save(refreshToken, "raw-token-value");

    const row = database.prepare("SELECT token_hash FROM refresh_tokens WHERE id = ?").get("token-1") as {
      token_hash: string;
    };
    expect(row.token_hash).not.toBe("raw-token-value");
  });

  it("revokes a token by id", async () => {
    const refreshToken = RefreshToken.issue({
      id: "token-1",
      userId: "user-1",
      expiresAt: EXPIRES_AT,
      createdAt: ISSUED_AT,
    });
    await repository.save(refreshToken, "raw-token-value");

    await repository.revoke("token-1", ISSUED_AT);

    const found = await repository.findByRawToken("raw-token-value");
    expect(found?.isRevoked()).toBe(true);
  });

  it("revokes all active tokens for a user, leaving other users untouched", async () => {
    const tokenA = RefreshToken.issue({ id: "token-a", userId: "user-1", expiresAt: EXPIRES_AT, createdAt: ISSUED_AT });
    const tokenB = RefreshToken.issue({ id: "token-b", userId: "user-1", expiresAt: EXPIRES_AT, createdAt: ISSUED_AT });
    const tokenC = RefreshToken.issue({ id: "token-c", userId: "user-2", expiresAt: EXPIRES_AT, createdAt: ISSUED_AT });
    await repository.save(tokenA, "raw-a");
    await repository.save(tokenB, "raw-b");
    await repository.save(tokenC, "raw-c");

    await repository.revokeAllForUser("user-1", ISSUED_AT);

    expect((await repository.findByRawToken("raw-a"))?.isRevoked()).toBe(true);
    expect((await repository.findByRawToken("raw-b"))?.isRevoked()).toBe(true);
    expect((await repository.findByRawToken("raw-c"))?.isRevoked()).toBe(false);
  });
});
