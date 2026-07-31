import { PasswordResetToken } from "../../domain/user/PasswordResetToken";

export interface PasswordResetTokenRepository {
  save(token: PasswordResetToken, rawTokenValue: string): Promise<void>;
  findByRawToken(rawTokenValue: string): Promise<PasswordResetToken | null>;
  markUsed(id: string, usedAt: Date): Promise<void>;
  invalidateAllForUser(userId: string, usedAt: Date): Promise<void>;
}
