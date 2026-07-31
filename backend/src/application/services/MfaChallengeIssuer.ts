import { MfaChallenge } from "../../domain/user/MfaChallenge";
import { MfaChallengeRepository } from "../ports/MfaChallengeRepository";
import { MfaChallengeGenerator } from "../ports/MfaChallengeGenerator";
import { IdGenerator } from "../ports/IdGenerator";
import { Clock } from "../ports/Clock";

export interface IssuedMfaChallenge {
  challengeToken: string;
}

/**
 * Issues a short-lived MFA challenge for a user — mirrors SessionIssuer
 * (ADR-0007) as a single-purpose collaborator, keeping AuthenticateUserUseCase's
 * constructor from growing by one port per new concern (ADR-0012).
 */
export class MfaChallengeIssuer {
  constructor(
    private readonly mfaChallengeRepository: MfaChallengeRepository,
    private readonly mfaChallengeGenerator: MfaChallengeGenerator,
    private readonly idGenerator: IdGenerator,
    private readonly clock: Clock,
    private readonly challengeTtlMs: number,
  ) {}

  async issueFor(userId: string): Promise<IssuedMfaChallenge> {
    const now = this.clock.now();
    const rawToken = this.mfaChallengeGenerator.generate();
    const challenge = MfaChallenge.issue({
      id: this.idGenerator.generate(),
      userId,
      createdAt: now,
      expiresAt: new Date(now.getTime() + this.challengeTtlMs),
    });

    await this.mfaChallengeRepository.save(challenge, rawToken);

    return { challengeToken: rawToken };
  }
}
