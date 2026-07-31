import { createHash } from "node:crypto";

/**
 * Password reset tokens are opaque, high-entropy secrets (ADR-0009): only
 * their SHA-256 hash is ever persisted, never the raw value.
 */
export function hashPasswordResetToken(rawTokenValue: string): string {
  return createHash("sha256").update(rawTokenValue).digest("hex");
}
