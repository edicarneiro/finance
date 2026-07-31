import { describe, expect, it } from "vitest";
import { Email } from "./Email";
import { InvalidEmailError } from "./errors/InvalidEmailError";

describe("Email", () => {
  it("creates a valid email normalized to lowercase and trimmed", () => {
    const email = Email.create(" User@Example.com ");

    expect(email.toString()).toBe("user@example.com");
  });

  it("rejects a value without an @ symbol", () => {
    expect(() => Email.create("invalid-email")).toThrow(InvalidEmailError);
  });

  it("rejects a value without a domain", () => {
    expect(() => Email.create("user@")).toThrow(InvalidEmailError);
  });

  it("rejects an empty value", () => {
    expect(() => Email.create("")).toThrow(InvalidEmailError);
  });

  it("considers two emails with different casing equal", () => {
    const a = Email.create("User@Example.com");
    const b = Email.create("user@example.com");

    expect(a.equals(b)).toBe(true);
  });
});
