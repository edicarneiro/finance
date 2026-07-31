import { RefreshToken } from "../../../domain/session/RefreshToken";
import { RefreshTokenRepository } from "../../../application/ports/RefreshTokenRepository";
import { hashRefreshToken } from "./refreshTokenHash";

export class InMemoryRefreshTokenRepository implements RefreshTokenRepository {
  private readonly tokensByHash = new Map<string, RefreshToken>();

  async save(refreshToken: RefreshToken, rawTokenValue: string): Promise<void> {
    this.tokensByHash.set(hashRefreshToken(rawTokenValue), refreshToken);
  }

  async findByRawToken(rawTokenValue: string): Promise<RefreshToken | null> {
    return this.tokensByHash.get(hashRefreshToken(rawTokenValue)) ?? null;
  }

  async revoke(id: string, revokedAt: Date): Promise<void> {
    for (const token of this.tokensByHash.values()) {
      if (token.id === id) {
        token.revoke(revokedAt);
      }
    }
  }

  async revokeAllForUser(userId: string, revokedAt: Date): Promise<void> {
    for (const token of this.tokensByHash.values()) {
      if (token.userId === userId && !token.isRevoked()) {
        token.revoke(revokedAt);
      }
    }
  }
}
