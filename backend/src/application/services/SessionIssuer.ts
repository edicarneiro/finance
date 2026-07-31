import { RefreshToken } from "../../domain/session/RefreshToken";
import { TokenService } from "../ports/TokenService";
import { RefreshTokenRepository } from "../ports/RefreshTokenRepository";
import { RefreshTokenGenerator } from "../ports/RefreshTokenGenerator";
import { IdGenerator } from "../ports/IdGenerator";
import { Clock } from "../ports/Clock";

export interface IssuedSession {
  accessToken: string;
  refreshToken: string;
}

/**
 * Issues a fresh access + refresh token pair for a user. Shared by
 * AuthenticateUserUseCase (login) and RefreshAccessTokenUseCase (renewal),
 * per ADR-0007, to avoid duplicating session-issuing logic.
 */
export class SessionIssuer {
  constructor(
    private readonly tokenService: TokenService,
    private readonly refreshTokenRepository: RefreshTokenRepository,
    private readonly refreshTokenGenerator: RefreshTokenGenerator,
    private readonly idGenerator: IdGenerator,
    private readonly clock: Clock,
    private readonly refreshTokenTtlMs: number,
  ) {}

  async issueFor(userId: string): Promise<IssuedSession> {
    const accessToken = this.tokenService.issue(userId);

    const now = this.clock.now();
    const rawRefreshToken = this.refreshTokenGenerator.generate();
    const refreshToken = RefreshToken.issue({
      id: this.idGenerator.generate(),
      userId,
      createdAt: now,
      expiresAt: new Date(now.getTime() + this.refreshTokenTtlMs),
    });

    await this.refreshTokenRepository.save(refreshToken, rawRefreshToken);

    return { accessToken, refreshToken: rawRefreshToken };
  }
}
