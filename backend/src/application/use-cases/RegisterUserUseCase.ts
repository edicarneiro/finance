import { Email } from "../../domain/user/Email";
import { User } from "../../domain/user/User";
import { assertStrongPassword } from "../../domain/user/PasswordPolicy";
import { DuplicateEmailError } from "../../domain/user/errors/DuplicateEmailError";
import { UserRepository } from "../ports/UserRepository";
import { PasswordHasher } from "../ports/PasswordHasher";
import { IdGenerator } from "../ports/IdGenerator";

export interface RegisterUserInput {
  email: string;
  password: string;
}

export interface RegisterUserOutput {
  userId: string;
}

export class RegisterUserUseCase {
  constructor(
    private readonly userRepository: UserRepository,
    private readonly passwordHasher: PasswordHasher,
    private readonly idGenerator: IdGenerator,
  ) {}

  async execute(input: RegisterUserInput): Promise<RegisterUserOutput> {
    const email = Email.create(input.email);
    assertStrongPassword(input.password);

    const existingUser = await this.userRepository.findByEmail(email);
    if (existingUser) {
      throw new DuplicateEmailError(email.toString());
    }

    const passwordHash = await this.passwordHasher.hash(input.password);
    const user = User.register({
      id: this.idGenerator.generate(),
      email,
      passwordHash,
    });

    await this.userRepository.save(user);

    return { userId: user.id };
  }
}
