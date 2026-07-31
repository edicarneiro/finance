export interface IssueMfaChallengeParams {
  id: string;
  userId: string;
  expiresAt: Date;
  createdAt?: Date;
}

export interface RestoreMfaChallengeParams {
  id: string;
  userId: string;
  expiresAt: Date;
  createdAt: Date;
  usedAt: Date | null;
}

/**
 * Proves "email/password were already validated" between the two steps of
 * MFA login (ADR-0012). Structurally near-identical to PasswordResetToken
 * (ADR-0009) — kept as its own entity for the same reason documented there
 * (generalizing now would mix a structural refactor with this feature).
 */
export class MfaChallenge {
  private constructor(
    public readonly id: string,
    public readonly userId: string,
    public readonly expiresAt: Date,
    public readonly createdAt: Date,
    private usedAt: Date | null,
  ) {}

  static issue(params: IssueMfaChallengeParams): MfaChallenge {
    return new MfaChallenge(params.id, params.userId, params.expiresAt, params.createdAt ?? new Date(), null);
  }

  static restore(params: RestoreMfaChallengeParams): MfaChallenge {
    return new MfaChallenge(params.id, params.userId, params.expiresAt, params.createdAt, params.usedAt);
  }

  isUsed(): boolean {
    return this.usedAt !== null;
  }

  isExpired(now: Date): boolean {
    return now.getTime() >= this.expiresAt.getTime();
  }

  isValid(now: Date): boolean {
    return !this.isUsed() && !this.isExpired(now);
  }

  markUsed(usedAt: Date): void {
    this.usedAt = usedAt;
  }
}
