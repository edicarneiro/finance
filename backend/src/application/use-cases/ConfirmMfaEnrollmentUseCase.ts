import { UserNotFoundError } from "../../domain/user/errors/UserNotFoundError";
import { MfaNotEnrolledError } from "../../domain/user/errors/MfaNotEnrolledError";
import { InvalidMfaCodeError } from "../../domain/user/errors/InvalidMfaCodeError";
import { UserRepository } from "../ports/UserRepository";
import { MfaCredentialRepository } from "../ports/MfaCredentialRepository";
import { TotpService } from "../ports/TotpService";
import { Clock } from "../ports/Clock";

export interface ConfirmMfaEnrollmentInput {
  userId: string;
  code: string;
}

export class ConfirmMfaEnrollmentUseCase {
  constructor(
    private readonly userRepository: UserRepository,
    private readonly mfaCredentialRepository: MfaCredentialRepository,
    private readonly totpService: TotpService,
    private readonly clock: Clock,
  ) {}

  async execute(input: ConfirmMfaEnrollmentInput): Promise<void> {
    const user = await this.userRepository.findById(input.userId);
    if (!user || user.isDeleted()) {
      throw new UserNotFoundError();
    }

    const credential = await this.mfaCredentialRepository.findByUserId(input.userId);
    if (!credential || credential.isConfirmed()) {
      throw new MfaNotEnrolledError();
    }

    const codeIsValid = await this.totpService.verify({ secret: credential.secret, token: input.code });
    if (!codeIsValid) {
      throw new InvalidMfaCodeError();
    }

    await this.mfaCredentialRepository.confirm(credential.id, this.clock.now());
  }
}
