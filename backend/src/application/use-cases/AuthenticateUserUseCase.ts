import { Email } from "../../domain/user/Email";
import { InvalidCredentialsError } from "../../domain/user/errors/InvalidCredentialsError";
import { UserRepository } from "../ports/UserRepository";
import { PasswordHasher } from "../ports/PasswordHasher";
import { MfaCredentialRepository } from "../ports/MfaCredentialRepository";
import { SessionIssuer } from "../services/SessionIssuer";
import { MfaChallengeIssuer } from "../services/MfaChallengeIssuer";

export interface AuthenticateUserInput {
  email: string;
  password: string;
}

export type AuthenticateUserOutput =
  | { mfaRequired: false; token: string; refreshToken: string }
  | { mfaRequired: true; challengeToken: string };

export class AuthenticateUserUseCase {
  constructor(
    private readonly userRepository: UserRepository,
    private readonly passwordHasher: PasswordHasher,
    private readonly sessionIssuer: SessionIssuer,
    private readonly mfaCredentialRepository: MfaCredentialRepository,
    private readonly mfaChallengeIssuer: MfaChallengeIssuer,
  ) {}

  async execute(input: AuthenticateUserInput): Promise<AuthenticateUserOutput> {
    const email = Email.create(input.email);
    const user = await this.userRepository.findByEmail(email);
    if (!user || user.isDeleted()) {
      throw new InvalidCredentialsError();
    }

    const passwordMatches = await this.passwordHasher.compare(input.password, user.passwordHash);
    if (!passwordMatches) {
      throw new InvalidCredentialsError();
    }

    const mfaCredential = await this.mfaCredentialRepository.findByUserId(user.id);
    if (mfaCredential?.isActive()) {
      const challenge = await this.mfaChallengeIssuer.issueFor(user.id);
      return { mfaRequired: true, challengeToken: challenge.challengeToken };
    }

    const session = await this.sessionIssuer.issueFor(user.id);
    return { mfaRequired: false, token: session.accessToken, refreshToken: session.refreshToken };
  }
}
