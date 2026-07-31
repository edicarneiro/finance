import { describe, expect, it } from "vitest";
import { RandomPasswordResetTokenGenerator } from "./RandomPasswordResetTokenGenerator";

describe("RandomPasswordResetTokenGenerator", () => {
  it("generates a high-entropy hex string (256 bits, per ADR-0009)", () => {
    const generator = new RandomPasswordResetTokenGenerator();

    expect(generator.generate()).toMatch(/^[0-9a-f]{64}$/);
  });

  it("generates a different value on each call", () => {
    const generator = new RandomPasswordResetTokenGenerator();

    expect(generator.generate()).not.toBe(generator.generate());
  });
});
