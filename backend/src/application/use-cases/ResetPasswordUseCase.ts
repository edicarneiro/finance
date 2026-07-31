import { assertStrongPassword } from "../../domain/user/PasswordPolicy";
import { InvalidOrExpiredResetTokenError } from "../../domain/user/errors/InvalidOrExpiredResetTokenError";
import { UserRepository } from "../ports/UserRepository";
import { PasswordResetTokenRepository } from "../ports/PasswordResetTokenRepository";
import { PasswordHasher } from "../ports/PasswordHasher";
import { RefreshTokenRepository } from "../ports/RefreshTokenRepository";
import { Clock } from "../ports/Clock";

export interface ResetPasswordInput {
  token: string;
  newPassword: string;
}

export class ResetPasswordUseCase {
  constructor(
    private readonly resetTokenRepository: PasswordResetTokenRepository,
    private readonly userRepository: UserRepository,
    private readonly passwordHasher: PasswordHasher,
    private readonly refreshTokenRepository: RefreshTokenRepository,
    private readonly clock: Clock,
  ) {}

  async execute(input: ResetPasswordInput): Promise<void> {
    const now = this.clock.now();
    const storedToken = await this.resetTokenRepository.findByRawToken(input.token);
    if (!storedToken || !storedToken.isValid(now)) {
      throw new InvalidOrExpiredResetTokenError();
    }

    // Validated before consuming the token, so a rejected password does not burn the user's only reset attempt.
    assertStrongPassword(input.newPassword);

    const user = await this.userRepository.findById(storedToken.userId);
    if (!user) {
      throw new InvalidOrExpiredResetTokenError();
    }

    const passwordHash = await this.passwordHasher.hash(input.newPassword);
    await this.userRepository.update(user.withPassword(passwordHash));

    await this.resetTokenRepository.markUsed(storedToken.id, now);
    // A password reset invalidates any session that may have been compromised (ADR-0009).
    await this.refreshTokenRepository.revokeAllForUser(user.id, now);
  }
}
