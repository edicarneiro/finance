import { Email } from "../../domain/user/Email";
import { User } from "../../domain/user/User";

export interface UserRepository {
  findByEmail(email: Email): Promise<User | null>;
  save(user: User): Promise<void>;
}
