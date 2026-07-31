import { describe, expect, it } from "vitest";
import { parseCredentials } from "./parseCredentials";
import { InvalidRequestBodyError } from "./errors/InvalidRequestBodyError";

describe("parseCredentials", () => {
  it("returns email and password when both are present strings", () => {
    const result = parseCredentials({ email: "user@example.com", password: "StrongPass1" });

    expect(result).toEqual({ email: "user@example.com", password: "StrongPass1" });
  });

  it("rejects a body without an email", () => {
    expect(() => parseCredentials({ password: "StrongPass1" })).toThrow(InvalidRequestBodyError);
  });

  it("rejects a body without a password", () => {
    expect(() => parseCredentials({ email: "user@example.com" })).toThrow(InvalidRequestBodyError);
  });

  it("rejects a body where email is not a string", () => {
    expect(() => parseCredentials({ email: 123, password: "StrongPass1" })).toThrow(InvalidRequestBodyError);
  });

  it("rejects an undefined body", () => {
    expect(() => parseCredentials(undefined)).toThrow(InvalidRequestBodyError);
  });
});
