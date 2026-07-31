import type Database from "better-sqlite3";
import { PasswordResetToken } from "../../../domain/user/PasswordResetToken";
import { PasswordResetTokenRepository } from "../../../application/ports/PasswordResetTokenRepository";
import { hashPasswordResetToken } from "./passwordResetTokenHash";

interface PasswordResetTokenRow {
  id: string;
  user_id: string;
  expires_at: string;
  created_at: string;
  used_at: string | null;
}

export class SqlitePasswordResetTokenRepository implements PasswordResetTokenRepository {
  constructor(private readonly db: Database.Database) {
    this.db.exec(`
      CREATE TABLE IF NOT EXISTS password_reset_tokens (
        id TEXT PRIMARY KEY,
        user_id TEXT NOT NULL,
        token_hash TEXT NOT NULL UNIQUE,
        expires_at TEXT NOT NULL,
        created_at TEXT NOT NULL,
        used_at TEXT
      )
    `);
  }

  async save(token: PasswordResetToken, rawTokenValue: string): Promise<void> {
    this.db
      .prepare(
        `INSERT INTO password_reset_tokens (id, user_id, token_hash, expires_at, created_at, used_at)
         VALUES (?, ?, ?, ?, ?, NULL)`,
      )
      .run(
        token.id,
        token.userId,
        hashPasswordResetToken(rawTokenValue),
        token.expiresAt.toISOString(),
        token.createdAt.toISOString(),
      );
  }

  async findByRawToken(rawTokenValue: string): Promise<PasswordResetToken | null> {
    const row = this.db
      .prepare("SELECT id, user_id, expires_at, created_at, used_at FROM password_reset_tokens WHERE token_hash = ?")
      .get(hashPasswordResetToken(rawTokenValue)) as PasswordResetTokenRow | undefined;

    return row ? this.toDomain(row) : null;
  }

  async markUsed(id: string, usedAt: Date): Promise<void> {
    this.db.prepare("UPDATE password_reset_tokens SET used_at = ? WHERE id = ?").run(usedAt.toISOString(), id);
  }

  async invalidateAllForUser(userId: string, usedAt: Date): Promise<void> {
    this.db
      .prepare("UPDATE password_reset_tokens SET used_at = ? WHERE user_id = ? AND used_at IS NULL")
      .run(usedAt.toISOString(), userId);
  }

  private toDomain(row: PasswordResetTokenRow): PasswordResetToken {
    return PasswordResetToken.restore({
      id: row.id,
      userId: row.user_id,
      expiresAt: new Date(row.expires_at),
      createdAt: new Date(row.created_at),
      usedAt: row.used_at ? new Date(row.used_at) : null,
    });
  }
}
