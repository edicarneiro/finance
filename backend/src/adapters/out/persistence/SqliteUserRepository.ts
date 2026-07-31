import type Database from "better-sqlite3";
import { Email } from "../../../domain/user/Email";
import { User } from "../../../domain/user/User";
import { UserRepository } from "../../../application/ports/UserRepository";

interface UserRow {
  id: string;
  email: string;
  password_hash: string;
  name: string | null;
  created_at: string;
  deleted_at: string | null;
}

export class SqliteUserRepository implements UserRepository {
  constructor(private readonly db: Database.Database) {
    this.db.exec(`
      CREATE TABLE IF NOT EXISTS users (
        id TEXT PRIMARY KEY,
        email TEXT NOT NULL UNIQUE,
        password_hash TEXT NOT NULL,
        name TEXT,
        created_at TEXT NOT NULL,
        deleted_at TEXT
      )
    `);
  }

  async findByEmail(email: Email): Promise<User | null> {
    const row = this.db
      .prepare("SELECT id, email, password_hash, name, created_at, deleted_at FROM users WHERE email = ?")
      .get(email.toString()) as UserRow | undefined;

    return row ? this.toDomain(row) : null;
  }

  async findById(id: string): Promise<User | null> {
    const row = this.db
      .prepare("SELECT id, email, password_hash, name, created_at, deleted_at FROM users WHERE id = ?")
      .get(id) as UserRow | undefined;

    return row ? this.toDomain(row) : null;
  }

  async save(user: User): Promise<void> {
    this.db
      .prepare("INSERT INTO users (id, email, password_hash, name, created_at, deleted_at) VALUES (?, ?, ?, ?, ?, ?)")
      .run(user.id, user.email.toString(), user.passwordHash, user.name, user.createdAt.toISOString(), user.deletedAt?.toISOString() ?? null);
  }

  async update(user: User): Promise<void> {
    this.db
      .prepare("UPDATE users SET email = ?, name = ?, password_hash = ?, deleted_at = ? WHERE id = ?")
      .run(user.email.toString(), user.name, user.passwordHash, user.deletedAt?.toISOString() ?? null, user.id);
  }

  private toDomain(row: UserRow): User {
    return User.register({
      id: row.id,
      email: Email.create(row.email),
      passwordHash: row.password_hash,
      name: row.name,
      createdAt: new Date(row.created_at),
      deletedAt: row.deleted_at ? new Date(row.deleted_at) : null,
    });
  }
}
