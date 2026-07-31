export interface IssuePasswordResetTokenParams {
  id: string;
  userId: string;
  expiresAt: Date;
  createdAt?: Date;
}

export interface RestorePasswordResetTokenParams {
  id: string;
  userId: string;
  expiresAt: Date;
  createdAt: Date;
  usedAt: Date | null;
}

/**
 * Structurally similar to session/RefreshToken (ADR-0007), kept as a
 * distinct entity rather than a shared abstraction — see ADR-0009
 * (generalizing now would be premature, "rule of three" not met).
 */
export class PasswordResetToken {
  private constructor(
    public readonly id: string,
    public readonly userId: string,
    public readonly expiresAt: Date,
    public readonly createdAt: Date,
    private usedAt: Date | null,
  ) {}

  static issue(params: IssuePasswordResetTokenParams): PasswordResetToken {
    return new PasswordResetToken(params.id, params.userId, params.expiresAt, params.createdAt ?? new Date(), null);
  }

  static restore(params: RestorePasswordResetTokenParams): PasswordResetToken {
    return new PasswordResetToken(params.id, params.userId, params.expiresAt, params.createdAt, params.usedAt);
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
