import { UserNotFoundError } from "../../domain/user/errors/UserNotFoundError";
import { InvalidCredentialsError } from "../../domain/user/errors/InvalidCredentialsError";
import { MfaNotEnrolledError } from "../../domain/user/errors/MfaNotEnrolledError";
import { UserRepository } from "../ports/UserRepository";
import { PasswordHasher } from "../ports/PasswordHasher";
import { MfaCredentialRepository } from "../ports/MfaCredentialRepository";
import { Clock } from "../ports/Clock";

export interface DisableMfaInput {
  userId: string;
  password: string;
}

export class DisableMfaUseCase {
  constructor(
    private readonly userRepository: UserRepository,
    private readonly passwordHasher: PasswordHasher,
    private readonly mfaCredentialRepository: MfaCredentialRepository,
    private readonly clock: Clock,
  ) {}

  async execute(input: DisableMfaInput): Promise<void> {
    const user = await this.userRepository.findById(input.userId);
    if (!user || user.isDeleted()) {
      throw new UserNotFoundError();
    }

    const passwordMatches = await this.passwordHasher.compare(input.password, user.passwordHash);
    if (!passwordMatches) {
      throw new InvalidCredentialsError();
    }

    const credential = await this.mfaCredentialRepository.findByUserId(input.userId);
    if (!credential || !credential.isActive()) {
      throw new MfaNotEnrolledError();
    }

    await this.mfaCredentialRepository.disable(credential.id, this.clock.now());
  }
}
