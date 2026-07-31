import type { RefreshTokenGenerator } from "../application/ports/RefreshTokenGenerator";

export class SequentialRefreshTokenGenerator implements RefreshTokenGenerator {
  private counter = 0;

  generate(): string {
    this.counter += 1;
    return `refresh-token-${this.counter}`;
  }
}
