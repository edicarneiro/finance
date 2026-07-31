import type Database from "better-sqlite3";
import { MfaChallenge } from "../../../domain/user/MfaChallenge";
import { MfaChallengeRepository } from "../../../application/ports/MfaChallengeRepository";
import { hashMfaChallengeToken } from "./mfaChallengeHash";

interface MfaChallengeRow {
  id: string;
  user_id: string;
  expires_at: string;
  created_at: string;
  used_at: string | null;
}

export class SqliteMfaChallengeRepository implements MfaChallengeRepository {
  constructor(private readonly db: Database.Database) {
    this.db.exec(`
      CREATE TABLE IF NOT EXISTS mfa_challenges (
        id TEXT PRIMARY KEY,
        user_id TEXT NOT NULL,
        token_hash TEXT NOT NULL UNIQUE,
        expires_at TEXT NOT NULL,
        created_at TEXT NOT NULL,
        used_at TEXT
      )
    `);
  }

  async save(challenge: MfaChallenge, rawTokenValue: string): Promise<void> {
    this.db
      .prepare(
        `INSERT INTO mfa_challenges (id, user_id, token_hash, expires_at, created_at, used_at)
         VALUES (?, ?, ?, ?, ?, NULL)`,
      )
      .run(
        challenge.id,
        challenge.userId,
        hashMfaChallengeToken(rawTokenValue),
        challenge.expiresAt.toISOString(),
        challenge.createdAt.toISOString(),
      );
  }

  async findByRawToken(rawTokenValue: string): Promise<MfaChallenge | null> {
    const row = this.db
      .prepare("SELECT id, user_id, expires_at, created_at, used_at FROM mfa_challenges WHERE token_hash = ?")
      .get(hashMfaChallengeToken(rawTokenValue)) as MfaChallengeRow | undefined;

    return row ? this.toDomain(row) : null;
  }

  async markUsed(id: string, usedAt: Date): Promise<void> {
    this.db.prepare("UPDATE mfa_challenges SET used_at = ? WHERE id = ?").run(usedAt.toISOString(), id);
  }

  private toDomain(row: MfaChallengeRow): MfaChallenge {
    return MfaChallenge.restore({
      id: row.id,
      userId: row.user_id,
      expiresAt: new Date(row.expires_at),
      createdAt: new Date(row.created_at),
      usedAt: row.used_at ? new Date(row.used_at) : null,
    });
  }
}
