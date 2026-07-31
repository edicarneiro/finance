import { Email } from "../../domain/user/Email";
import { InvalidCredentialsError } from "../../domain/user/errors/InvalidCredentialsError";
import { UserRepository } from "../ports/UserRepository";
import { PasswordHasher } from "../ports/PasswordHasher";
import { TokenService } from "../ports/TokenService";

export interface AuthenticateUserInput {
  email: string;
  password: string;
}

export interface AuthenticateUserOutput {
  token: string;
}

export class AuthenticateUserUseCase {
  constructor(
    private readonly userRepository: UserRepository,
    private readonly passwordHasher: PasswordHasher,
    private readonly tokenService: TokenService,
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

    const token = this.tokenService.issue(user.id);
    return { token };
  }
}
