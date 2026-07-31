import type Database from "better-sqlite3";
import { RefreshToken } from "../../../domain/session/RefreshToken";
import { RefreshTokenRepository } from "../../../application/ports/RefreshTokenRepository";
import { hashRefreshToken } from "./refreshTokenHash";

interface RefreshTokenRow {
  id: string;
  user_id: string;
  expires_at: string;
  created_at: string;
  revoked_at: string | null;
}

export class SqliteRefreshTokenRepository implements RefreshTokenRepository {
  constructor(private readonly db: Database.Database) {
    this.db.exec(`
      CREATE TABLE IF NOT EXISTS refresh_tokens (
        id TEXT PRIMARY KEY,
        user_id TEXT NOT NULL,
        token_hash TEXT NOT NULL UNIQUE,
        expires_at TEXT NOT NULL,
        created_at TEXT NOT NULL,
        revoked_at TEXT
      )
    `);
  }

  async save(refreshToken: RefreshToken, rawTokenValue: string): Promise<void> {
    this.db
      .prepare(
        `INSERT INTO refresh_tokens (id, user_id, token_hash, expires_at, created_at, revoked_at)
         VALUES (?, ?, ?, ?, ?, NULL)`,
      )
      .run(
        refreshToken.id,
        refreshToken.userId,
        hashRefreshToken(rawTokenValue),
        refreshToken.expiresAt.toISOString(),
        refreshToken.createdAt.toISOString(),
      );
  }

  async findByRawToken(rawTokenValue: string): Promise<RefreshToken | null> {
    const row = this.db
      .prepare("SELECT id, user_id, expires_at, created_at, revoked_at FROM refresh_tokens WHERE token_hash = ?")
      .get(hashRefreshToken(rawTokenValue)) as RefreshTokenRow | undefined;

    return row ? this.toDomain(row) : null;
  }

  async revoke(id: string, revokedAt: Date): Promise<void> {
    this.db.prepare("UPDATE refresh_tokens SET revoked_at = ? WHERE id = ?").run(revokedAt.toISOString(), id);
  }

  async revokeAllForUser(userId: string, revokedAt: Date): Promise<void> {
    this.db
      .prepare("UPDATE refresh_tokens SET revoked_at = ? WHERE user_id = ? AND revoked_at IS NULL")
      .run(revokedAt.toISOString(), userId);
  }

  private toDomain(row: RefreshTokenRow): RefreshToken {
    return RefreshToken.restore({
      id: row.id,
      userId: row.user_id,
      expiresAt: new Date(row.expires_at),
      createdAt: new Date(row.created_at),
      revokedAt: row.revoked_at ? new Date(row.revoked_at) : null,
    });
  }
}
