import { createHash } from "node:crypto";

/** MFA challenge tokens are opaque, high-entropy secrets (ADR-0012): only their SHA-256 hash is ever persisted. */
export function hashMfaChallengeToken(rawTokenValue: string): string {
  return createHash("sha256").update(rawTokenValue).digest("hex");
}
