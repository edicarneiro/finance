export interface EnrollMfaCredentialParams {
  id: string;
  userId: string;
  secret: string;
  createdAt?: Date;
}

export interface RestoreMfaCredentialParams {
  id: string;
  userId: string;
  secret: string;
  createdAt: Date;
  confirmedAt: Date | null;
  disabledAt: Date | null;
}

/**
 * At most one per user (ADR-0011) — a fresh enroll() replaces any prior
 * credential, confirmed or not. `secret` is always the plaintext TOTP seed
 * from the domain's perspective; encryption at rest is an adapter concern
 * (SqliteMfaCredentialRepository), never leaked here.
 */
export class MfaCredential {
  private constructor(
    public readonly id: string,
    public readonly userId: string,
    public readonly secret: string,
    public readonly createdAt: Date,
    private confirmedAt: Date | null,
    private disabledAt: Date | null,
  ) {}

  static enroll(params: EnrollMfaCredentialParams): MfaCredential {
    return new MfaCredential(params.id, params.userId, params.secret, params.createdAt ?? new Date(), null, null);
  }

  static restore(params: RestoreMfaCredentialParams): MfaCredential {
    return new MfaCredential(
      params.id,
      params.userId,
      params.secret,
      params.createdAt,
      params.confirmedAt,
      params.disabledAt,
    );
  }

  isConfirmed(): boolean {
    return this.confirmedAt !== null;
  }

  isDisabled(): boolean {
    return this.disabledAt !== null;
  }

  isActive(): boolean {
    return this.isConfirmed() && !this.isDisabled();
  }

  confirm(confirmedAt: Date): void {
    this.confirmedAt = confirmedAt;
  }

  disable(disabledAt: Date): void {
    this.disabledAt = disabledAt;
  }
}
