import type Database from "better-sqlite3";
import { MfaCredential } from "../../../domain/user/MfaCredential";
import { MfaCredentialRepository } from "../../../application/ports/MfaCredentialRepository";
import { SecretCipher } from "../../../application/ports/SecretCipher";

interface MfaCredentialRow {
  id: string;
  user_id: string;
  secret_ciphertext: string;
  created_at: string;
  confirmed_at: string | null;
  disabled_at: string | null;
}

export class SqliteMfaCredentialRepository implements MfaCredentialRepository {
  constructor(
    private readonly db: Database.Database,
    private readonly secretCipher: SecretCipher,
  ) {
    this.db.exec(`
      CREATE TABLE IF NOT EXISTS mfa_credentials (
        id TEXT PRIMARY KEY,
        user_id TEXT NOT NULL UNIQUE,
        secret_ciphertext TEXT NOT NULL,
        created_at TEXT NOT NULL,
        confirmed_at TEXT,
        disabled_at TEXT
      )
    `);
  }

  async findByUserId(userId: string): Promise<MfaCredential | null> {
    const row = this.db
      .prepare(
        "SELECT id, user_id, secret_ciphertext, created_at, confirmed_at, disabled_at FROM mfa_credentials WHERE user_id = ?",
      )
      .get(userId) as MfaCredentialRow | undefined;

    return row ? this.toDomain(row) : null;
  }

  async save(credential: MfaCredential): Promise<void> {
    // A UNIQUE constraint on user_id makes this a per-user replace (ADR-0011).
    this.db
      .prepare(
        `INSERT OR REPLACE INTO mfa_credentials (id, user_id, secret_ciphertext, created_at, confirmed_at, disabled_at)
         VALUES (?, ?, ?, ?, NULL, NULL)`,
      )
      .run(credential.id, credential.userId, this.secretCipher.encrypt(credential.secret), credential.createdAt.toISOString());
  }

  async confirm(id: string, confirmedAt: Date): Promise<void> {
    this.db.prepare("UPDATE mfa_credentials SET confirmed_at = ? WHERE id = ?").run(confirmedAt.toISOString(), id);
  }

  async disable(id: string, disabledAt: Date): Promise<void> {
    this.db.prepare("UPDATE mfa_credentials SET disabled_at = ? WHERE id = ?").run(disabledAt.toISOString(), id);
  }

  private toDomain(row: MfaCredentialRow): MfaCredential {
    return MfaCredential.restore({
      id: row.id,
      userId: row.user_id,
      secret: this.secretCipher.decrypt(row.secret_ciphertext),
      createdAt: new Date(row.created_at),
      confirmedAt: row.confirmed_at ? new Date(row.confirmed_at) : null,
      disabledAt: row.disabled_at ? new Date(row.disabled_at) : null,
    });
  }
}
