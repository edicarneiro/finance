import { Email } from "../../domain/user/Email";
import { UserNotFoundError } from "../../domain/user/errors/UserNotFoundError";
import { InvalidCredentialsError } from "../../domain/user/errors/InvalidCredentialsError";
import { UserRepository } from "../ports/UserRepository";
import { PasswordHasher } from "../ports/PasswordHasher";
import { RefreshTokenRepository } from "../ports/RefreshTokenRepository";
import { PasswordResetTokenRepository } from "../ports/PasswordResetTokenRepository";
import { MfaCredentialRepository } from "../ports/MfaCredentialRepository";
import { IdGenerator } from "../ports/IdGenerator";
import { Clock } from "../ports/Clock";

export interface DeleteAccountInput {
  userId: string;
  password: string;
}

/**
 * Anonymizes the account rather than deleting the row (RF-007, ADR-0010) —
 * data-retention obligations are an open legal question (vision.md §17.1.7),
 * so a hard, irreversible delete is avoided until that is resolved.
 */
export class DeleteAccountUseCase {
  constructor(
    private readonly userRepository: UserRepository,
    private readonly passwordHasher: PasswordHasher,
    private readonly refreshTokenRepository: RefreshTokenRepository,
    private readonly passwordResetTokenRepository: PasswordResetTokenRepository,
    private readonly mfaCredentialRepository: MfaCredentialRepository,
    private readonly idGenerator: IdGenerator,
    private readonly clock: Clock,
  ) {}

  async execute(input: DeleteAccountInput): Promise<void> {
    const user = await this.userRepository.findById(input.userId);
    if (!user) {
      throw new UserNotFoundError();
    }

    // "Confirmação explícita" (RF-007): the current password must be re-entered.
    const passwordMatches = await this.passwordHasher.compare(input.password, user.passwordHash);
    if (!passwordMatches) {
      throw new InvalidCredentialsError();
    }

    const now = this.clock.now();
    const anonymizedEmail = Email.create(`deleted-${this.idGenerator.generate()}@anonymized.financepulse.internal`);
    const anonymizedPasswordHash = await this.passwordHasher.hash(this.idGenerator.generate());

    const anonymizedUser = user.anonymize({
      email: anonymizedEmail,
      passwordHash: anonymizedPasswordHash,
      deletedAt: now,
    });

    await this.userRepository.update(anonymizedUser);
    await this.refreshTokenRepository.revokeAllForUser(user.id, now);
    await this.passwordResetTokenRepository.invalidateAllForUser(user.id, now);

    // Closes a real gap found in QA review (Fase 2.5.2): without this, a
    // pending MFA login challenge issued before deletion could still be
    // completed afterwards, handing out a fresh session for a deleted account.
    const mfaCredential = await this.mfaCredentialRepository.findByUserId(user.id);
    if (mfaCredential?.isActive()) {
      await this.mfaCredentialRepository.disable(mfaCredential.id, now);
    }
  }
}
