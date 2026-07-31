import { MfaCredential } from "../../domain/user/MfaCredential";
import { UserNotFoundError } from "../../domain/user/errors/UserNotFoundError";
import { InvalidCredentialsError } from "../../domain/user/errors/InvalidCredentialsError";
import { UserRepository } from "../ports/UserRepository";
import { PasswordHasher } from "../ports/PasswordHasher";
import { MfaCredentialRepository } from "../ports/MfaCredentialRepository";
import { TotpService } from "../ports/TotpService";
import { IdGenerator } from "../ports/IdGenerator";
import { Clock } from "../ports/Clock";

export interface EnrollMfaInput {
  userId: string;
  password: string;
}

export interface EnrollMfaOutput {
  secret: string;
  otpauthUrl: string;
}

/**
 * Requires the current password as explicit confirmation (ADR-0011, same
 * rationale as RF-007/ADR-0010): otherwise a still-valid stolen access token
 * could be used to plant an attacker-controlled MFA secret.
 */
export class EnrollMfaUseCase {
  constructor(
    private readonly userRepository: UserRepository,
    private readonly passwordHasher: PasswordHasher,
    private readonly mfaCredentialRepository: MfaCredentialRepository,
    private readonly totpService: TotpService,
    private readonly idGenerator: IdGenerator,
    private readonly clock: Clock,
  ) {}

  async execute(input: EnrollMfaInput): Promise<EnrollMfaOutput> {
    const user = await this.userRepository.findById(input.userId);
    if (!user || user.isDeleted()) {
      throw new UserNotFoundError();
    }

    const passwordMatches = await this.passwordHasher.compare(input.password, user.passwordHash);
    if (!passwordMatches) {
      throw new InvalidCredentialsError();
    }

    const secret = this.totpService.generateSecret();
    const credential = MfaCredential.enroll({
      id: this.idGenerator.generate(),
      userId: user.id,
      secret,
      createdAt: this.clock.now(),
    });
    await this.mfaCredentialRepository.save(credential);

    const otpauthUrl = this.totpService.buildOtpauthUrl({ secret, accountName: user.email.toString() });
    return { secret, otpauthUrl };
  }
}
