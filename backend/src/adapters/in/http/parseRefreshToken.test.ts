import { describe, expect, it } from "vitest";
import { parseRefreshToken } from "./parseRefreshToken";
import { InvalidRequestBodyError } from "./errors/InvalidRequestBodyError";

describe("parseRefreshToken", () => {
  it("returns the refresh token when present as a string", () => {
    expect(parseRefreshToken({ refreshToken: "abc123" })).toBe("abc123");
  });

  it("rejects a body without a refreshToken field", () => {
    expect(() => parseRefreshToken({})).toThrow(InvalidRequestBodyError);
  });

  it("rejects a body where refreshToken is not a string", () => {
    expect(() => parseRefreshToken({ refreshToken: 123 })).toThrow(InvalidRequestBodyError);
  });

  it("rejects an undefined body", () => {
    expect(() => parseRefreshToken(undefined)).toThrow(InvalidRequestBodyError);
  });
});
