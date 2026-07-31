import { describe, expect, it } from "vitest";
import { User } from "./User";
import { Email } from "./Email";

describe("User", () => {
  it("registers a user with the provided data and a default creation timestamp", () => {
    const email = Email.create("user@example.com");

    const user = User.register({ id: "user-1", email, passwordHash: "hashed-value" });

    expect(user.id).toBe("user-1");
    expect(user.email.equals(email)).toBe(true);
    expect(user.passwordHash).toBe("hashed-value");
    expect(user.createdAt).toBeInstanceOf(Date);
  });

  it("accepts an explicit creation timestamp", () => {
    const createdAt = new Date("2026-01-01T00:00:00.000Z");

    const user = User.register({
      id: "user-1",
      email: Email.create("user@example.com"),
      passwordHash: "hashed-value",
      createdAt,
    });

    expect(user.createdAt).toBe(createdAt);
  });

  it("has no name by default (RF-001 does not collect a name at registration)", () => {
    const user = User.register({ id: "user-1", email: Email.create("user@example.com"), passwordHash: "hashed" });

    expect(user.name).toBeNull();
  });

  it("registers a user with an explicit name when provided", () => {
    const user = User.register({
      id: "user-1",
      email: Email.create("user@example.com"),
      passwordHash: "hashed",
      name: "Ana Souza",
    });

    expect(user.name).toBe("Ana Souza");
  });

  it("produces a new User with an updated name and email, preserving id/password/createdAt (RF-006)", () => {
    const createdAt = new Date("2026-01-01T00:00:00.000Z");
    const user = User.register({
      id: "user-1",
      email: Email.create("old@example.com"),
      passwordHash: "hashed-value",
      createdAt,
    });

    const newEmail = Email.create("new@example.com");
    const updated = user.withProfile({ name: "Ana Souza", email: newEmail });

    expect(updated.id).toBe("user-1");
    expect(updated.passwordHash).toBe("hashed-value");
    expect(updated.createdAt).toBe(createdAt);
    expect(updated.name).toBe("Ana Souza");
    expect(updated.email.equals(newEmail)).toBe(true);
    // withProfile does not mutate the original instance.
    expect(user.name).toBeNull();
    expect(user.email.equals(newEmail)).toBe(false);
  });

  it("produces a new User with an updated password hash, preserving everything else (RF-005)", () => {
    const createdAt = new Date("2026-01-01T00:00:00.000Z");
    const email = Email.create("user@example.com");
    const user = User.register({ id: "user-1", email, passwordHash: "old-hash", name: "Ana Souza", createdAt });

    const updated = user.withPassword("new-hash");

    expect(updated.passwordHash).toBe("new-hash");
    expect(updated.id).toBe("user-1");
    expect(updated.email.equals(email)).toBe(true);
    expect(updated.name).toBe("Ana Souza");
    expect(updated.createdAt).toBe(createdAt);
    // withPassword does not mutate the original instance.
    expect(user.passwordHash).toBe("old-hash");
  });

  it("is not deleted by default", () => {
    const user = User.register({ id: "user-1", email: Email.create("user@example.com"), passwordHash: "hashed" });

    expect(user.isDeleted()).toBe(false);
    expect(user.deletedAt).toBeNull();
  });

  it("anonymizes a user, replacing email/name/password and recording deletedAt (RF-007)", () => {
    const createdAt = new Date("2026-01-01T00:00:00.000Z");
    const deletedAt = new Date("2026-02-01T00:00:00.000Z");
    const user = User.register({
      id: "user-1",
      email: Email.create("user@example.com"),
      passwordHash: "hashed-value",
      name: "Ana Souza",
      createdAt,
    });

    const anonymizedEmail = Email.create("deleted-user-1@anonymized.financepulse.internal");
    const anonymized = user.anonymize({ email: anonymizedEmail, passwordHash: "unusable-hash", deletedAt });

    expect(anonymized.id).toBe("user-1");
    expect(anonymized.email.equals(anonymizedEmail)).toBe(true);
    expect(anonymized.name).toBeNull();
    expect(anonymized.passwordHash).toBe("unusable-hash");
    expect(anonymized.deletedAt).toBe(deletedAt);
    expect(anonymized.isDeleted()).toBe(true);
    expect(anonymized.createdAt).toBe(createdAt);
    // anonymize does not mutate the original instance.
    expect(user.isDeleted()).toBe(false);
  });
});
