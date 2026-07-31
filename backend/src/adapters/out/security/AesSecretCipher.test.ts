import { describe, expect, it } from "vitest";
import { randomBytes } from "node:crypto";
import { AesSecretCipher } from "./AesSecretCipher";

describe("AesSecretCipher", () => {
  it("decrypts back to the original plaintext", () => {
    const cipher = new AesSecretCipher(randomBytes(32));

    const ciphertext = cipher.encrypt("MY_TOTP_SECRET");

    expect(ciphertext).not.toContain("MY_TOTP_SECRET");
    expect(cipher.decrypt(ciphertext)).toBe("MY_TOTP_SECRET");
  });

  it("produces a different ciphertext for the same plaintext on each call (random IV)", () => {
    const cipher = new AesSecretCipher(randomBytes(32));

    const first = cipher.encrypt("MY_TOTP_SECRET");
    const second = cipher.encrypt("MY_TOTP_SECRET");

    expect(first).not.toBe(second);
  });

  it("fails to decrypt with the wrong key (authentication tag mismatch)", () => {
    const ciphertext = new AesSecretCipher(randomBytes(32)).encrypt("MY_TOTP_SECRET");
    const wrongKeyCipher = new AesSecretCipher(randomBytes(32));

    expect(() => wrongKeyCipher.decrypt(ciphertext)).toThrow();
  });

  it("rejects a key shorter than 32 bytes at construction time", () => {
    expect(() => new AesSecretCipher(randomBytes(16))).toThrow(/32-byte/);
  });

  it("rejects a key longer than 32 bytes at construction time", () => {
    expect(() => new AesSecretCipher(randomBytes(64))).toThrow(/32-byte/);
  });
});
