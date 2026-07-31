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
});
