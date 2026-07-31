import { randomBytes } from "node:crypto";
import { PasswordResetTokenGenerator } from "../../../application/ports/PasswordResetTokenGenerator";

const TOKEN_BYTES = 32; // 256 bits

export class RandomPasswordResetTokenGenerator implements PasswordResetTokenGenerator {
  generate(): string {
    return randomBytes(TOKEN_BYTES).toString("hex");
  }
}
