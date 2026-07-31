import { describe, expect, it } from "vitest";
import { parseProfileUpdate } from "./parseProfileUpdate";
import { InvalidRequestBodyError } from "./errors/InvalidRequestBodyError";

describe("parseProfileUpdate", () => {
  it("returns name and email when both are present strings", () => {
    expect(parseProfileUpdate({ name: "Ana Souza", email: "ana@example.com" })).toEqual({
      name: "Ana Souza",
      email: "ana@example.com",
    });
  });

  it("rejects a body without a name", () => {
    expect(() => parseProfileUpdate({ email: "ana@example.com" })).toThrow(InvalidRequestBodyError);
  });

  it("rejects a body without an email", () => {
    expect(() => parseProfileUpdate({ name: "Ana Souza" })).toThrow(InvalidRequestBodyError);
  });

  it("rejects an undefined body", () => {
    expect(() => parseProfileUpdate(undefined)).toThrow(InvalidRequestBodyError);
  });
});
