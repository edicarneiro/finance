import { describe, expect, it } from "vitest";
import { parsePasswordResetConfirmation } from "./parsePasswordResetConfirmation";
import { InvalidRequestBodyError } from "./errors/InvalidRequestBodyError";

describe("parsePasswordResetConfirmation", () => {
  it("returns token and newPassword when both are present strings", () => {
    expect(parsePasswordResetConfirmation({ token: "abc", newPassword: "NewStrongPass1" })).toEqual({
      token: "abc",
      newPassword: "NewStrongPass1",
    });
  });

  it("rejects a body without a token", () => {
    expect(() => parsePasswordResetConfirmation({ newPassword: "NewStrongPass1" })).toThrow(InvalidRequestBodyError);
  });

  it("rejects a body without a newPassword", () => {
    expect(() => parsePasswordResetConfirmation({ token: "abc" })).toThrow(InvalidRequestBodyError);
  });

  it("rejects an undefined body", () => {
    expect(() => parsePasswordResetConfirmation(undefined)).toThrow(InvalidRequestBodyError);
  });
});
