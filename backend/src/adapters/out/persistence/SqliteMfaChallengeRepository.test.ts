import { afterEach, beforeEach, describe, expect, it } from "vitest";
import Database from "better-sqlite3";
import { SqliteMfaChallengeRepository } from "./SqliteMfaChallengeRepository";
import { MfaChallenge } from "../../../domain/user/MfaChallenge";

const ISSUED_AT = new Date("2026-01-01T00:00:00.000Z");
const EXPIRES_AT = new Date("2026-01-01T00:05:00.000Z");

describe("SqliteMfaChallengeRepository", () => {
  let database: Database.Database;
  let repository: SqliteMfaChallengeRepository;

  beforeEach(() => {
    database = new Database(":memory:");
    repository = new SqliteMfaChallengeRepository(database);
  });

  afterEach(() => {
    database.close();
  });

  it("returns null for a raw token that was never stored", async () => {
    expect(await repository.findByRawToken("never-issued")).toBeNull();
  });

  it("persists a challenge by its hash and retrieves it by the raw value", async () => {
    const challenge = MfaChallenge.issue({ id: "challenge-1", userId: "user-1", expiresAt: EXPIRES_AT, createdAt: ISSUED_AT });

    await repository.save(challenge, "raw-token-value");

    const found = await repository.findByRawToken("raw-token-value");
    expect(found?.id).toBe("challenge-1");
    expect(found?.userId).toBe("user-1");
    expect(found?.isUsed()).toBe(false);
  });

  it("never stores the raw token value as plain text", async () => {
    const challenge = MfaChallenge.issue({ id: "challenge-1", userId: "user-1", expiresAt: EXPIRES_AT, createdAt: ISSUED_AT });
    await repository.save(challenge, "raw-token-value");

    const row = database.prepare("SELECT token_hash FROM mfa_challenges WHERE id = ?").get("challenge-1") as {
      token_hash: string;
    };
    expect(row.token_hash).not.toBe("raw-token-value");
  });

  it("marks a challenge as used by id", async () => {
    const challenge = MfaChallenge.issue({ id: "challenge-1", userId: "user-1", expiresAt: EXPIRES_AT, createdAt: ISSUED_AT });
    await repository.save(challenge, "raw-token-value");

    await repository.markUsed("challenge-1", ISSUED_AT);

    expect((await repository.findByRawToken("raw-token-value"))?.isUsed()).toBe(true);
  });
});
