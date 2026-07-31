import { describe, expect, it } from "vitest";
import { BcryptPasswordHasher } from "./BcryptPasswordHasher";

describe("BcryptPasswordHasher", () => {
  it("hashes a password and verifies it back correctly", async () => {
    const hasher = new BcryptPasswordHasher();

    const hash = await hasher.hash("StrongPass1");

    expect(hash).not.toBe("StrongPass1");
    await expect(hasher.compare("StrongPass1", hash)).resolves.toBe(true);
    await expect(hasher.compare("WrongPass1", hash)).resolves.toBe(false);
  });

  it("produces a different hash for the same password on each call (random salt)", async () => {
    const hasher = new BcryptPasswordHasher();

    const first = await hasher.hash("StrongPass1");
    const second = await hasher.hash("StrongPass1");

    expect(first).not.toBe(second);
  });
});
