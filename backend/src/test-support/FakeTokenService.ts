import type { TokenPayload, TokenService } from "../application/ports/TokenService";

export class FakeTokenService implements TokenService {
  issue(userId: string): string {
    return `token-for-${userId}`;
  }

  verify(token: string): TokenPayload | null {
    const match = /^token-for-(.+)$/.exec(token);
    return match ? { userId: match[1] } : null;
  }
}
