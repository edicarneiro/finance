import { describe, expect, it } from "vitest";
import { parseMfaCode } from "./parseMfaCode";
import { InvalidRequestBodyError } from "./errors/InvalidRequestBodyError";

describe("parseMfaCode", () => {
  it("returns the code when present as a string", () => {
    expect(parseMfaCode({ code: "123456" })).toBe("123456");
  });

  it("rejects a body without a code field", () => {
    expect(() => parseMfaCode({})).toThrow(InvalidRequestBodyError);
  });

  it("rejects an undefined body", () => {
    expect(() => parseMfaCode(undefined)).toThrow(InvalidRequestBodyError);
  });
});
