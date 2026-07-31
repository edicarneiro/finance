export interface TokenPayload {
  userId: string;
}

export interface TokenService {
  issue(userId: string): string;
  verify(token: string): TokenPayload | null;
}
