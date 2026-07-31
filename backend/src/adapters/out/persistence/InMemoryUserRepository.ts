import { Email } from "../../../domain/user/Email";
import { User } from "../../../domain/user/User";
import { UserRepository } from "../../../application/ports/UserRepository";

export class InMemoryUserRepository implements UserRepository {
  private readonly usersByEmail = new Map<string, User>();

  async findByEmail(email: Email): Promise<User | null> {
    return this.usersByEmail.get(email.toString()) ?? null;
  }

  async save(user: User): Promise<void> {
    this.usersByEmail.set(user.email.toString(), user);
  }
}
