import { MfaChallenge } from "../../domain/user/MfaChallenge";

export interface MfaChallengeRepository {
  save(challenge: MfaChallenge, rawTokenValue: string): Promise<void>;
  findByRawToken(rawTokenValue: string): Promise<MfaChallenge | null>;
  markUsed(id: string, usedAt: Date): Promise<void>;
}
