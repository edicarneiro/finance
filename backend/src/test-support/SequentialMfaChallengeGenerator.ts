import type { MfaChallengeGenerator } from "../application/ports/MfaChallengeGenerator";

export class SequentialMfaChallengeGenerator implements MfaChallengeGenerator {
  private counter = 0;

  generate(): string {
    this.counter += 1;
    return `mfa-challenge-${this.counter}`;
  }
}
