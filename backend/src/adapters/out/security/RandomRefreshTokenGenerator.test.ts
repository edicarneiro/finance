import { describe, expect, it } from "vitest";
import { RandomRefreshTokenGenerator } from "./RandomRefreshTokenGenerator";

describe("RandomRefreshTokenGenerator", () => {
  it("generates a high-entropy hex string (256 bits, per ADR-0007)", () => {
    const generator = new RandomRefreshTokenGenerator();

    const token = generator.generate();

    expect(token).toMatch(/^[0-9a-f]{64}$/);
  });

  it("generates a different value on each call", () => {
    const generator = new RandomRefreshTokenGenerator();

    expect(generator.generate()).not.toBe(generator.generate());
  });
});
