import { afterEach, beforeEach, describe, expect, it } from "vitest";
import Database from "better-sqlite3";
import { randomBytes } from "node:crypto";
import { SqliteMfaCredentialRepository } from "./SqliteMfaCredentialRepository";
import { AesSecretCipher } from "../security/AesSecretCipher";
import { MfaCredential } from "../../../domain/user/MfaCredential";

const CREATED_AT = new Date("2026-01-01T00:00:00.000Z");

describe("SqliteMfaCredentialRepository", () => {
  let database: Database.Database;
  let repository: SqliteMfaCredentialRepository;

  beforeEach(() => {
    database = new Database(":memory:");
    repository = new SqliteMfaCredentialRepository(database, new AesSecretCipher(randomBytes(32)));
  });

  afterEach(() => {
    database.close();
  });

  it("returns null when the user has no MFA credential", async () => {
    expect(await repository.findByUserId("user-1")).toBeNull();
  });

  it("persists a credential and retrieves the same plaintext secret back", async () => {
    const credential = MfaCredential.enroll({ id: "mfa-1", userId: "user-1", secret: "JBSWY3DPEHPK3PXP", createdAt: CREATED_AT });

    await repository.save(credential);

    const found = await repository.findByUserId("user-1");
    expect(found?.secret).toBe("JBSWY3DPEHPK3PXP");
    expect(found?.isConfirmed()).toBe(false);
  });

  it("never stores the secret as plain text", async () => {
    const credential = MfaCredential.enroll({ id: "mfa-1", userId: "user-1", secret: "JBSWY3DPEHPK3PXP", createdAt: CREATED_AT });
    await repository.save(credential);

    const row = database.prepare("SELECT secret_ciphertext FROM mfa_credentials WHERE id = ?").get("mfa-1") as {
      secret_ciphertext: string;
    };
    expect(row.secret_ciphertext).not.toContain("JBSWY3DPEHPK3PXP");
  });

  it("replaces any prior credential for the same user (ADR-0011)", async () => {
    await repository.save(MfaCredential.enroll({ id: "mfa-1", userId: "user-1", secret: "SECRET-A", createdAt: CREATED_AT }));

    await repository.save(MfaCredential.enroll({ id: "mfa-2", userId: "user-1", secret: "SECRET-B", createdAt: CREATED_AT }));

    const found = await repository.findByUserId("user-1");
    expect(found?.id).toBe("mfa-2");
    expect(found?.secret).toBe("SECRET-B");
  });

  it("persists confirmation state via confirm()", async () => {
    const credential = MfaCredential.enroll({ id: "mfa-1", userId: "user-1", secret: "SECRET", createdAt: CREATED_AT });
    await repository.save(credential);

    await repository.confirm("mfa-1", CREATED_AT);

    const found = await repository.findByUserId("user-1");
    expect(found?.isActive()).toBe(true);
  });

  it("persists disabled state via disable()", async () => {
    const credential = MfaCredential.enroll({ id: "mfa-1", userId: "user-1", secret: "SECRET", createdAt: CREATED_AT });
    await repository.save(credential);
    await repository.confirm("mfa-1", CREATED_AT);

    await repository.disable("mfa-1", CREATED_AT);

    const found = await repository.findByUserId("user-1");
    expect(found?.isDisabled()).toBe(true);
    expect(found?.isActive()).toBe(false);
  });
});
