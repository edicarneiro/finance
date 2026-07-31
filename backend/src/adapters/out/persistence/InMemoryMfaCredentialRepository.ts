import { MfaCredential } from "../../../domain/user/MfaCredential";
import { MfaCredentialRepository } from "../../../application/ports/MfaCredentialRepository";

export class InMemoryMfaCredentialRepository implements MfaCredentialRepository {
  private readonly credentialsByUserId = new Map<string, MfaCredential>();

  async findByUserId(userId: string): Promise<MfaCredential | null> {
    return this.credentialsByUserId.get(userId) ?? null;
  }

  async save(credential: MfaCredential): Promise<void> {
    this.credentialsByUserId.set(credential.userId, credential);
  }

  async confirm(id: string, confirmedAt: Date): Promise<void> {
    for (const credential of this.credentialsByUserId.values()) {
      if (credential.id === id) {
        credential.confirm(confirmedAt);
      }
    }
  }

  async disable(id: string, disabledAt: Date): Promise<void> {
    for (const credential of this.credentialsByUserId.values()) {
      if (credential.id === id) {
        credential.disable(disabledAt);
      }
    }
  }
}
