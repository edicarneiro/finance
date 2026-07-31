import { RefreshTokenRepository } from "../ports/RefreshTokenRepository";
import { Clock } from "../ports/Clock";

export interface LogoutInput {
  refreshToken: string;
}

/**
 * Idempotent by design (RF-008): the goal is "this token must no longer
 * work", which holds whether it existed, was already revoked, or never did.
 */
export class LogoutUseCase {
  constructor(
    private readonly refreshTokenRepository: RefreshTokenRepository,
    private readonly clock: Clock,
  ) {}

  async execute(input: LogoutInput): Promise<void> {
    const storedToken = await this.refreshTokenRepository.findByRawToken(input.refreshToken);

    if (storedToken && !storedToken.isRevoked()) {
      await this.refreshTokenRepository.revoke(storedToken.id, this.clock.now());
    }
  }
}
