import { describe, expect, it } from "vitest";
import { assertValidName } from "./ProfilePolicy";
import { InvalidNameError } from "./errors/InvalidNameError";

describe("assertValidName", () => {
  it("accepts a non-empty trimmed name", () => {
    expect(() => assertValidName("Ana Souza")).not.toThrow();
  });

  it("rejects an empty name", () => {
    expect(() => assertValidName("")).toThrow(InvalidNameError);
  });

  it("rejects a name that is only whitespace", () => {
    expect(() => assertValidName("   ")).toThrow(InvalidNameError);
  });

  it("rejects a name longer than 100 characters", () => {
    expect(() => assertValidName("a".repeat(101))).toThrow(InvalidNameError);
  });

  it("accepts a name exactly at the 100 character limit", () => {
    expect(() => assertValidName("a".repeat(100))).not.toThrow();
  });
});
