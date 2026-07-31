import { describe, expect, it } from "vitest";
import { assertValidConsentVersion } from "./ConsentPolicy";
import { InvalidConsentVersionError } from "./errors/InvalidConsentVersionError";

describe("assertValidConsentVersion", () => {
  it("accepts a non-empty version identifier", () => {
    expect(() => assertValidConsentVersion("privacy-policy-v1")).not.toThrow();
  });

  it("rejects an empty version", () => {
    expect(() => assertValidConsentVersion("")).toThrow(InvalidConsentVersionError);
  });

  it("rejects a version that is only whitespace", () => {
    expect(() => assertValidConsentVersion("   ")).toThrow(InvalidConsentVersionError);
  });
});
