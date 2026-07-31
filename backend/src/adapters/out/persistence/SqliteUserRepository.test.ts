import { afterEach, beforeEach, describe, expect, it } from "vitest";
import Database from "better-sqlite3";
import { SqliteUserRepository } from "./SqliteUserRepository";
import { User } from "../../../domain/user/User";
import { Email } from "../../../domain/user/Email";

describe("SqliteUserRepository", () => {
  let database: Database.Database;
  let repository: SqliteUserRepository;

  beforeEach(() => {
    database = new Database(":memory:");
    repository = new SqliteUserRepository(database);
  });

  afterEach(() => {
    database.close();
  });

  it("returns null when the email is not registered", async () => {
    const found = await repository.findByEmail(Email.create("missing@example.com"));

    expect(found).toBeNull();
  });

  it("persists a user and retrieves it by email regardless of casing", async () => {
    const user = User.register({
      id: "user-1",
      email: Email.create("user@example.com"),
      passwordHash: "hashed-value",
    });

    await repository.save(user);

    const found = await repository.findByEmail(Email.create("USER@example.com"));
    expect(found?.id).toBe("user-1");
    expect(found?.passwordHash).toBe("hashed-value");
  });

  it("enforces email uniqueness at the schema level (RF-002)", async () => {
    const email = Email.create("user@example.com");
    await repository.save(User.register({ id: "user-1", email, passwordHash: "a" }));

    await expect(repository.save(User.register({ id: "user-2", email, passwordHash: "b" }))).rejects.toThrow();
  });
});
