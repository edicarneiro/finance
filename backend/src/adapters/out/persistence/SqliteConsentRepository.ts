import type Database from "better-sqlite3";
import { ConsentRecord } from "../../../domain/user/ConsentRecord";
import { ConsentRepository } from "../../../application/ports/ConsentRepository";

interface ConsentRow {
  id: string;
  user_id: string;
  version: string;
  accepted_at: string;
}

export class SqliteConsentRepository implements ConsentRepository {
  constructor(private readonly db: Database.Database) {
    this.db.exec(`
      CREATE TABLE IF NOT EXISTS consent_records (
        id TEXT PRIMARY KEY,
        user_id TEXT NOT NULL,
        version TEXT NOT NULL,
        accepted_at TEXT NOT NULL
      )
    `);
  }

  async save(record: ConsentRecord): Promise<void> {
    this.db
      .prepare("INSERT INTO consent_records (id, user_id, version, accepted_at) VALUES (?, ?, ?, ?)")
      .run(record.id, record.userId, record.version, record.acceptedAt.toISOString());
  }

  async findAllForUser(userId: string): Promise<ConsentRecord[]> {
    const rows = this.db
      .prepare("SELECT id, user_id, version, accepted_at FROM consent_records WHERE user_id = ? ORDER BY accepted_at ASC")
      .all(userId) as ConsentRow[];

    return rows.map((row) =>
      ConsentRecord.give({
        id: row.id,
        userId: row.user_id,
        version: row.version,
        acceptedAt: new Date(row.accepted_at),
      }),
    );
  }
}
