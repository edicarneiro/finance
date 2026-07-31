import { randomBytes } from "node:crypto";
import { RefreshTokenGenerator } from "../../../application/ports/RefreshTokenGenerator";

const TOKEN_BYTES = 32; // 256 bits

export class RandomRefreshTokenGenerator implements RefreshTokenGenerator {
  generate(): string {
    return randomBytes(TOKEN_BYTES).toString("hex");
  }
}
