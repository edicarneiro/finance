import { Email } from "./Email";

export interface RegisterUserParams {
  id: string;
  email: Email;
  passwordHash: string;
  createdAt?: Date;
}

export class User {
  private constructor(
    public readonly id: string,
    public readonly email: Email,
    public readonly passwordHash: string,
    public readonly createdAt: Date,
  ) {}

  static register(params: RegisterUserParams): User {
    return new User(params.id, params.email, params.passwordHash, params.createdAt ?? new Date());
  }
}
