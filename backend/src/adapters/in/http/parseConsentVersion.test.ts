import { describe, expect, it } from "vitest";
import { parseConsentVersion } from "./parseConsentVersion";
import { InvalidRequestBodyError } from "./errors/InvalidRequestBodyError";

describe("parseConsentVersion", () => {
  it("returns the version when present as a string", () => {
    expect(parseConsentVersion({ version: "privacy-policy-v1" })).toBe("privacy-policy-v1");
  });

  it("rejects a body without a version field", () => {
    expect(() => parseConsentVersion({})).toThrow(InvalidRequestBodyError);
  });

  it("rejects an undefined body", () => {
    expect(() => parseConsentVersion(undefined)).toThrow(InvalidRequestBodyError);
  });
});
