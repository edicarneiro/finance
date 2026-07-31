import { describe, expect, it } from "vitest";
import { RandomMfaChallengeGenerator } from "./RandomMfaChallengeGenerator";

describe("RandomMfaChallengeGenerator", () => {
  it("generates a high-entropy hex string (256 bits, per ADR-0012)", () => {
    const generator = new RandomMfaChallengeGenerator();

    expect(generator.generate()).toMatch(/^[0-9a-f]{64}$/);
  });

  it("generates a different value on each call", () => {
    const generator = new RandomMfaChallengeGenerator();

    expect(generator.generate()).not.toBe(generator.generate());
  });
});
