import { describe, expect, it } from "vitest";
import { parsePasswordConfirmation } from "./parsePasswordConfirmation";
import { InvalidRequestBodyError } from "./errors/InvalidRequestBodyError";

describe("parsePasswordConfirmation", () => {
  it("returns the password when present as a string", () => {
    expect(parsePasswordConfirmation({ password: "StrongPass1" })).toBe("StrongPass1");
  });

  it("rejects a body without a password field", () => {
    expect(() => parsePasswordConfirmation({})).toThrow(InvalidRequestBodyError);
  });

  it("rejects an undefined body", () => {
    expect(() => parsePasswordConfirmation(undefined)).toThrow(InvalidRequestBodyError);
  });
});
