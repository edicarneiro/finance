import { Email } from "../../domain/user/Email";
import { PasswordResetToken } from "../../domain/user/PasswordResetToken";
import { UserRepository } from "../ports/UserRepository";
import { PasswordResetTokenRepository } from "../ports/PasswordResetTokenRepository";
import { PasswordResetTokenGenerator } from "../ports/PasswordResetTokenGenerator";
import { PasswordResetNotifier } from "../ports/PasswordResetNotifier";
import { IdGenerator } from "../ports/IdGenerator";
import { Clock } from "../ports/Clock";

export interface RequestPasswordResetInput {
  email: string;
}

/**
 * Always resolves without error, whether or not the email is registered
 * (ADR-0009) — the caller must not be able to distinguish the two cases.
 */
export class RequestPasswordResetUseCase {
  constructor(
    private readonly userRepository: UserRepository,
    private readonly tokenRepository: PasswordResetTokenRepository,
    private readonly tokenGenerator: PasswordResetTokenGenerator,
    private readonly idGenerator: IdGenerator,
    private readonly clock: Clock,
    private readonly notifier: PasswordResetNotifier,
    private readonly tokenTtlMs: number,
  ) {}

  async execute(input: RequestPasswordResetInput): Promise<void> {
    const email = Email.create(input.email);
    const user = await this.userRepository.findByEmail(email);
    if (!user) {
      return;
    }

    const now = this.clock.now();
    await this.tokenRepository.invalidateAllForUser(user.id, now);

    const rawToken = this.tokenGenerator.generate();
    const token = PasswordResetToken.issue({
      id: this.idGenerator.generate(),
      userId: user.id,
      createdAt: now,
      expiresAt: new Date(now.getTime() + this.tokenTtlMs),
    });

    await this.tokenRepository.save(token, rawToken);
    await this.notifier.notifyPasswordResetRequested({ email, rawToken, expiresAt: token.expiresAt });
  }
}
