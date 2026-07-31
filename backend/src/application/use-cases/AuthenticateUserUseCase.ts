import { Email } from "../../domain/user/Email";
import { InvalidCredentialsError } from "../../domain/user/errors/InvalidCredentialsError";
import { UserRepository } from "../ports/UserRepository";
import { PasswordHasher } from "../ports/PasswordHasher";
import { SessionIssuer } from "../services/SessionIssuer";

export interface AuthenticateUserInput {
  email: string;
  password: string;
}

export interface AuthenticateUserOutput {
  token: string;
  refreshToken: string;
}

export class AuthenticateUserUseCase {
  constructor(
    private readonly userRepository: UserRepository,
    private readonly passwordHasher: PasswordHasher,
    private readonly sessionIssuer: SessionIssuer,
  ) {}

  async execute(input: AuthenticateUserInput): Promise<AuthenticateUserOutput> {
    const email = Email.create(input.email);
    const user = await this.userRepository.findByEmail(email);
    if (!user) {
      throw new InvalidCredentialsError();
    }

    const passwordMatches = await this.passwordHasher.compare(input.password, user.passwordHash);
    if (!passwordMatches) {
      throw new InvalidCredentialsError();
    }

    const session = await this.sessionIssuer.issueFor(user.id);
    return { token: session.accessToken, refreshToken: session.refreshToken };
  }
}
