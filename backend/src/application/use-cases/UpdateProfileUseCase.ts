import { Email } from "../../domain/user/Email";
import { assertValidName } from "../../domain/user/ProfilePolicy";
import { UserNotFoundError } from "../../domain/user/errors/UserNotFoundError";
import { DuplicateEmailError } from "../../domain/user/errors/DuplicateEmailError";
import { UserRepository } from "../ports/UserRepository";
import { ProfileView } from "./GetProfileUseCase";

export interface UpdateProfileInput {
  userId: string;
  name: string;
  email: string;
}

export class UpdateProfileUseCase {
  constructor(private readonly userRepository: UserRepository) {}

  async execute(input: UpdateProfileInput): Promise<ProfileView> {
    const user = await this.userRepository.findById(input.userId);
    if (!user) {
      throw new UserNotFoundError();
    }

    assertValidName(input.name);
    const email = Email.create(input.email);

    if (!email.equals(user.email)) {
      const existing = await this.userRepository.findByEmail(email);
      if (existing) {
        throw new DuplicateEmailError(email.toString());
      }
    }

    const updated = user.withProfile({ name: input.name.trim(), email });
    await this.userRepository.update(updated);

    return { id: updated.id, name: updated.name, email: updated.email.toString() };
  }
}
