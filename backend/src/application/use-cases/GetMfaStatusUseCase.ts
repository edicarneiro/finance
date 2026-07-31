import { UserNotFoundError } from "../../domain/user/errors/UserNotFoundError";
import { UserRepository } from "../ports/UserRepository";
import { MfaCredentialRepository } from "../ports/MfaCredentialRepository";

export interface GetMfaStatusInput {
  userId: string;
}

export interface MfaStatusView {
  enabled: boolean;
}

export class GetMfaStatusUseCase {
  constructor(
    private readonly userRepository: UserRepository,
    private readonly mfaCredentialRepository: MfaCredentialRepository,
  ) {}

  async execute(input: GetMfaStatusInput): Promise<MfaStatusView> {
    const user = await this.userRepository.findById(input.userId);
    if (!user || user.isDeleted()) {
      throw new UserNotFoundError();
    }

    const credential = await this.mfaCredentialRepository.findByUserId(input.userId);
    return { enabled: credential?.isActive() ?? false };
  }
}
