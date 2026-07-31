import type { PasswordResetTokenGenerator } from "../application/ports/PasswordResetTokenGenerator";

export class SequentialPasswordResetTokenGenerator implements PasswordResetTokenGenerator {
  private counter = 0;

  generate(): string {
    this.counter += 1;
    return `reset-token-${this.counter}`;
  }
}
