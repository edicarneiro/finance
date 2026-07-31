import { createHash } from "node:crypto";

/**
 * Refresh tokens are opaque, high-entropy secrets (ADR-0007): only their
 * SHA-256 hash is ever persisted, never the raw value.
 */
export function hashRefreshToken(rawTokenValue: string): string {
  return createHash("sha256").update(rawTokenValue).digest("hex");
}
