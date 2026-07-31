import { createCipheriv, createDecipheriv, randomBytes } from "node:crypto";
import { SecretCipher } from "../../../application/ports/SecretCipher";

const ALGORITHM = "aes-256-gcm";
const IV_LENGTH = 12;

/**
 * Encrypts secrets at rest (ADR-0011) — unlike refresh/reset tokens (SHA-256
 * hash, one-way, compare-only), a TOTP secret must be recovered in plaintext
 * to verify future codes, so hashing is not applicable here.
 */
export class AesSecretCipher implements SecretCipher {
  constructor(private readonly key: Buffer) {
    if (key.length !== 32) {
      throw new Error("AesSecretCipher requires a 32-byte (256-bit) key.");
    }
  }

  encrypt(plaintext: string): string {
    const iv = randomBytes(IV_LENGTH);
    const cipher = createCipheriv(ALGORITHM, this.key, iv);
    const ciphertext = Buffer.concat([cipher.update(plaintext, "utf8"), cipher.final()]);
    const authTag = cipher.getAuthTag();

    return [iv, authTag, ciphertext].map((buffer) => buffer.toString("hex")).join(":");
  }

  decrypt(ciphertext: string): string {
    const [ivHex, authTagHex, dataHex] = ciphertext.split(":");
    const decipher = createDecipheriv(ALGORITHM, this.key, Buffer.from(ivHex, "hex"));
    decipher.setAuthTag(Buffer.from(authTagHex, "hex"));

    return Buffer.concat([decipher.update(Buffer.from(dataHex, "hex")), decipher.final()]).toString("utf8");
  }
}
