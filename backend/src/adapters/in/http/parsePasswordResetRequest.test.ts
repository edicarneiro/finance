import { describe, expect, it } from "vitest";
import { parsePasswordResetRequest } from "./parsePasswordResetRequest";
import { InvalidRequestBodyError } from "./errors/InvalidRequestBodyError";

describe("parsePasswordResetRequest", () => {
  it("returns the email when present as a string", () => {
    expect(parsePasswordResetRequest({ email: "user@example.com" })).toBe("user@example.com");
  });

  it("rejects a body without an email field", () => {
    expect(() => parsePasswordResetRequest({})).toThrow(InvalidRequestBodyError);
  });

  it("rejects an undefined body", () => {
    expect(() => parsePasswordResetRequest(undefined)).toThrow(InvalidRequestBodyError);
  });
});
