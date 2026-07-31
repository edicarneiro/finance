export interface IssueRefreshTokenParams {
  id: string;
  userId: string;
  expiresAt: Date;
  createdAt?: Date;
}

export interface RestoreRefreshTokenParams {
  id: string;
  userId: string;
  expiresAt: Date;
  createdAt: Date;
  revokedAt: Date | null;
}

export class RefreshToken {
  private constructor(
    public readonly id: string,
    public readonly userId: string,
    public readonly expiresAt: Date,
    public readonly createdAt: Date,
    private revokedAt: Date | null,
  ) {}

  static issue(params: IssueRefreshTokenParams): RefreshToken {
    return new RefreshToken(params.id, params.userId, params.expiresAt, params.createdAt ?? new Date(), null);
  }

  static restore(params: RestoreRefreshTokenParams): RefreshToken {
    return new RefreshToken(params.id, params.userId, params.expiresAt, params.createdAt, params.revokedAt);
  }

  isRevoked(): boolean {
    return this.revokedAt !== null;
  }

  isExpired(now: Date): boolean {
    return now.getTime() >= this.expiresAt.getTime();
  }

  isValid(now: Date): boolean {
    return !this.isRevoked() && !this.isExpired(now);
  }

  revoke(revokedAt: Date): void {
    this.revokedAt = revokedAt;
  }
}
