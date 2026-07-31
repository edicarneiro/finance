export interface GiveConsentParams {
  id: string;
  userId: string;
  version: string;
  acceptedAt?: Date;
}

/**
 * Append-only audit record (RF-046, ADR-0008): a consent, once given, is
 * never mutated or deleted — history is preserved in full.
 */
export class ConsentRecord {
  private constructor(
    public readonly id: string,
    public readonly userId: string,
    public readonly version: string,
    public readonly acceptedAt: Date,
  ) {}

  static give(params: GiveConsentParams): ConsentRecord {
    return new ConsentRecord(params.id, params.userId, params.version, params.acceptedAt ?? new Date());
  }
}
