import { UserNotFoundError } from "../../domain/user/errors/UserNotFoundError";
import { UserRepository } from "../ports/UserRepository";

export interface GetProfileInput {
  userId: string;
}

export interface ProfileView {
  id: string;
  name: string | null;
  email: string;
}

export class GetProfileUseCase {
  constructor(private readonly userRepository: UserRepository) {}

  async execute(input: GetProfileInput): Promise<ProfileView> {
    const user = await this.userRepository.findById(input.userId);
    if (!user) {
      throw new UserNotFoundError();
    }

    return { id: user.id, name: user.name, email: user.email.toString() };
  }
}
