import { PasswordResetToken } from "../../../domain/user/PasswordResetToken";
import { PasswordResetTokenRepository } from "../../../application/ports/PasswordResetTokenRepository";
import { hashPasswordResetToken } from "./passwordResetTokenHash";

export class InMemoryPasswordResetTokenRepository implements PasswordResetTokenRepository {
  private readonly tokensByHash = new Map<string, PasswordResetToken>();

  async save(token: PasswordResetToken, rawTokenValue: string): Promise<void> {
    this.tokensByHash.set(hashPasswordResetToken(rawTokenValue), token);
  }

  async findByRawToken(rawTokenValue: string): Promise<PasswordResetToken | null> {
    return this.tokensByHash.get(hashPasswordResetToken(rawTokenValue)) ?? null;
  }

  async markUsed(id: string, usedAt: Date): Promise<void> {
    for (const token of this.tokensByHash.values()) {
      if (token.id === id) {
        token.markUsed(usedAt);
      }
    }
  }

  async invalidateAllForUser(userId: string, usedAt: Date): Promise<void> {
    for (const token of this.tokensByHash.values()) {
      if (token.userId === userId && !token.isUsed()) {
        token.markUsed(usedAt);
      }
    }
  }
}
