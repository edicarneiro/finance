import { afterEach, beforeEach, describe, expect, it } from "vitest";
import Database from "better-sqlite3";
import { SqliteConsentRepository } from "./SqliteConsentRepository";
import { ConsentRecord } from "../../../domain/user/ConsentRecord";

const ACCEPTED_AT = new Date("2026-01-01T00:00:00.000Z");

describe("SqliteConsentRepository", () => {
  let database: Database.Database;
  let repository: SqliteConsentRepository;

  beforeEach(() => {
    database = new Database(":memory:");
    repository = new SqliteConsentRepository(database);
  });

  afterEach(() => {
    database.close();
  });

  it("returns an empty list for a user with no consent records", async () => {
    expect(await repository.findAllForUser("user-1")).toEqual([]);
  });

  it("persists multiple consent records for the same user without overwriting previous ones (RF-046)", async () => {
    await repository.save(
      ConsentRecord.give({ id: "consent-1", userId: "user-1", version: "privacy-policy-v1", acceptedAt: ACCEPTED_AT }),
    );
    await repository.save(
      ConsentRecord.give({ id: "consent-2", userId: "user-1", version: "privacy-policy-v2", acceptedAt: ACCEPTED_AT }),
    );

    const records = await repository.findAllForUser("user-1");
    expect(records).toHaveLength(2);
    expect(records.map((record) => record.version).sort()).toEqual(["privacy-policy-v1", "privacy-policy-v2"]);
  });

  it("does not mix consent records between users", async () => {
    await repository.save(
      ConsentRecord.give({ id: "consent-1", userId: "user-1", version: "privacy-policy-v1", acceptedAt: ACCEPTED_AT }),
    );
    await repository.save(
      ConsentRecord.give({ id: "consent-2", userId: "user-2", version: "privacy-policy-v1", acceptedAt: ACCEPTED_AT }),
    );

    expect(await repository.findAllForUser("user-1")).toHaveLength(1);
  });
});
