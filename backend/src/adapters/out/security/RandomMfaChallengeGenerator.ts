import { randomBytes } from "node:crypto";
import { MfaChallengeGenerator } from "../../../application/ports/MfaChallengeGenerator";

const TOKEN_BYTES = 32; // 256 bits

export class RandomMfaChallengeGenerator implements MfaChallengeGenerator {
  generate(): string {
    return randomBytes(TOKEN_BYTES).toString("hex");
  }
}
