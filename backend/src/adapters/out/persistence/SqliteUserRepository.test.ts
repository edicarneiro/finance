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

  it("returns null from findById when the user does not exist", async () => {
    expect(await repository.findById("missing")).toBeNull();
  });

  it("persists and retrieves a user's name, defaulting to null when absent (RF-006)", async () => {
    await repository.save(User.register({ id: "user-1", email: Email.create("user@example.com"), passwordHash: "a" }));

    const found = await repository.findById("user-1");
    expect(found?.name).toBeNull();
  });

  it("updates a user's name and email in place (RF-006)", async () => {
    const email = Email.create("user@example.com");
    const user = User.register({ id: "user-1", email, passwordHash: "hashed-value" });
    await repository.save(user);

    const newEmail = Email.create("new@example.com");
    await repository.update(user.withProfile({ name: "Ana Souza", email: newEmail }));

    const found = await repository.findById("user-1");
    expect(found?.name).toBe("Ana Souza");
    expect(found?.email.equals(newEmail)).toBe(true);
    // The row count must remain 1 — update() must not insert a second row.
    expect(await repository.findByEmail(email)).toBeNull();
  });

  it("persists a password hash change made via update() (RF-005)", async () => {
    const email = Email.create("user@example.com");
    const user = User.register({ id: "user-1", email, passwordHash: "old-hash" });
    await repository.save(user);

    await repository.update(user.withPassword("new-hash"));

    const found = await repository.findById("user-1");
    expect(found?.passwordHash).toBe("new-hash");
  });

  it("does not revert name/email when update() is called only to change the password", async () => {
    const email = Email.create("user@example.com");
    const user = User.register({ id: "user-1", email, passwordHash: "hashed-value" });
    await repository.save(user);
    await repository.update(user.withProfile({ name: "Ana Souza", email }));

    const withUpdatedProfile = await repository.findById("user-1");
    await repository.update(withUpdatedProfile!.withPassword("new-hash"));

    const found = await repository.findById("user-1");
    expect(found?.name).toBe("Ana Souza");
    expect(found?.passwordHash).toBe("new-hash");
  });
});
