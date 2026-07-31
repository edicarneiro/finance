import { MfaCredential } from "../../domain/user/MfaCredential";

export interface MfaCredentialRepository {
  findByUserId(userId: string): Promise<MfaCredential | null>;
  /** Upserts by userId — a fresh enrollment always replaces any prior credential (ADR-0011). */
  save(credential: MfaCredential): Promise<void>;
  confirm(id: string, confirmedAt: Date): Promise<void>;
  disable(id: string, disabledAt: Date): Promise<void>;
}
