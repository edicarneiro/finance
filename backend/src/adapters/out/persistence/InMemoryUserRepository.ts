import { Email } from "../../../domain/user/Email";
import { User } from "../../../domain/user/User";
import { UserRepository } from "../../../application/ports/UserRepository";

export class InMemoryUserRepository implements UserRepository {
  private readonly usersById = new Map<string, User>();

  async findByEmail(email: Email): Promise<User | null> {
    for (const user of this.usersById.values()) {
      if (user.email.equals(email)) {
        return user;
      }
    }
    return null;
  }

  async findById(id: string): Promise<User | null> {
    return this.usersById.get(id) ?? null;
  }

  async save(user: User): Promise<void> {
    this.usersById.set(user.id, user);
  }

  async update(user: User): Promise<void> {
    this.usersById.set(user.id, user);
  }
}
