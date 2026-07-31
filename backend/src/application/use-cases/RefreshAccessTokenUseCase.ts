import { InvalidRefreshTokenError } from "../../domain/session/errors/InvalidRefreshTokenError";
import { RefreshTokenRepository } from "../ports/RefreshTokenRepository";
import { Clock } from "../ports/Clock";
import { SessionIssuer } from "../services/SessionIssuer";

export interface RefreshAccessTokenInput {
  refreshToken: string;
}

export interface RefreshAccessTokenOutput {
  token: string;
  refreshToken: string;
}

export class RefreshAccessTokenUseCase {
  constructor(
    private readonly refreshTokenRepository: RefreshTokenRepository,
    private readonly sessionIssuer: SessionIssuer,
    private readonly clock: Clock,
  ) {}

  async execute(input: RefreshAccessTokenInput): Promise<RefreshAccessTokenOutput> {
    const now = this.clock.now();
    const storedToken = await this.refreshTokenRepository.findByRawToken(input.refreshToken);

    if (!storedToken) {
      throw new InvalidRefreshTokenError();
    }

    if (storedToken.isRevoked()) {
      // A previously rotated/revoked token being presented again is a signal
      // of possible theft (ADR-0007) — revoke every session for this user.
      await this.refreshTokenRepository.revokeAllForUser(storedToken.userId, now);
      throw new InvalidRefreshTokenError();
    }

    if (storedToken.isExpired(now)) {
      throw new InvalidRefreshTokenError();
    }

    await this.refreshTokenRepository.revoke(storedToken.id, now);

    const session = await this.sessionIssuer.issueFor(storedToken.userId);
    return { token: session.accessToken, refreshToken: session.refreshToken };
  }
}
