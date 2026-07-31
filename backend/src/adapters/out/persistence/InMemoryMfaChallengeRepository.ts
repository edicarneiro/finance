import { MfaChallenge } from "../../../domain/user/MfaChallenge";
import { MfaChallengeRepository } from "../../../application/ports/MfaChallengeRepository";
import { hashMfaChallengeToken } from "./mfaChallengeHash";

export class InMemoryMfaChallengeRepository implements MfaChallengeRepository {
  private readonly challengesByHash = new Map<string, MfaChallenge>();

  async save(challenge: MfaChallenge, rawTokenValue: string): Promise<void> {
    this.challengesByHash.set(hashMfaChallengeToken(rawTokenValue), challenge);
  }

  async findByRawToken(rawTokenValue: string): Promise<MfaChallenge | null> {
    return this.challengesByHash.get(hashMfaChallengeToken(rawTokenValue)) ?? null;
  }

  async markUsed(id: string, usedAt: Date): Promise<void> {
    for (const challenge of this.challengesByHash.values()) {
      if (challenge.id === id) {
        challenge.markUsed(usedAt);
      }
    }
  }
}
