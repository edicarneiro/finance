import type Database from "better-sqlite3";
import { Email } from "../../../domain/user/Email";
import { User } from "../../../domain/user/User";
import { UserRepository } from "../../../application/ports/UserRepository";

interface UserRow {
  id: string;
  email: string;
  password_hash: string;
  created_at: string;
}

export class SqliteUserRepository implements UserRepository {
  constructor(private readonly db: Database.Database) {
    this.db.exec(`
      CREATE TABLE IF NOT EXISTS users (
        id TEXT PRIMARY KEY,
        email TEXT NOT NULL UNIQUE,
        password_hash TEXT NOT NULL,
        created_at TEXT NOT NULL
      )
    `);
  }

  async findByEmail(email: Email): Promise<User | null> {
    const row = this.db.prepare("SELECT id, email, password_hash, created_at FROM users WHERE email = ?").get(
      email.toString(),
    ) as UserRow | undefined;

    return row ? this.toDomain(row) : null;
  }

  async save(user: User): Promise<void> {
    this.db
      .prepare("INSERT INTO users (id, email, password_hash, created_at) VALUES (?, ?, ?, ?)")
      .run(user.id, user.email.toString(), user.passwordHash, user.createdAt.toISOString());
  }

  private toDomain(row: UserRow): User {
    return User.register({
      id: row.id,
      email: Email.create(row.email),
      passwordHash: row.password_hash,
      createdAt: new Date(row.created_at),
    });
  }
}
