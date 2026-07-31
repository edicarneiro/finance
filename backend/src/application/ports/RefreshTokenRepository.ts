import { RefreshToken } from "../../domain/session/RefreshToken";

export interface RefreshTokenRepository {
  save(refreshToken: RefreshToken, rawTokenValue: string): Promise<void>;
  findByRawToken(rawTokenValue: string): Promise<RefreshToken | null>;
  revoke(id: string, revokedAt: Date): Promise<void>;
  revokeAllForUser(userId: string, revokedAt: Date): Promise<void>;
}
