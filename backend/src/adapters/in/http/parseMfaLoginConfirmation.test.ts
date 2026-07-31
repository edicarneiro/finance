import { describe, expect, it } from "vitest";
import { parseMfaLoginConfirmation } from "./parseMfaLoginConfirmation";
import { InvalidRequestBodyError } from "./errors/InvalidRequestBodyError";

describe("parseMfaLoginConfirmation", () => {
  it("returns challengeToken and code when both are present strings", () => {
    expect(parseMfaLoginConfirmation({ challengeToken: "abc", code: "123456" })).toEqual({
      challengeToken: "abc",
      code: "123456",
    });
  });

  it("rejects a body without a challengeToken", () => {
    expect(() => parseMfaLoginConfirmation({ code: "123456" })).toThrow(InvalidRequestBodyError);
  });

  it("rejects a body without a code", () => {
    expect(() => parseMfaLoginConfirmation({ challengeToken: "abc" })).toThrow(InvalidRequestBodyError);
  });

  it("rejects an undefined body", () => {
    expect(() => parseMfaLoginConfirmation(undefined)).toThrow(InvalidRequestBodyError);
  });
});
