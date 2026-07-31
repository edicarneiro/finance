import { Email } from "./Email";

export interface RegisterUserParams {
  id: string;
  email: Email;
  passwordHash: string;
  name?: string | null;
  createdAt?: Date;
}

export interface UpdateUserProfileParams {
  name: string;
  email: Email;
}

export class User {
  private constructor(
    public readonly id: string,
    public readonly email: Email,
    public readonly passwordHash: string,
    public readonly name: string | null,
    public readonly createdAt: Date,
  ) {}

  static register(params: RegisterUserParams): User {
    return new User(
      params.id,
      params.email,
      params.passwordHash,
      params.name ?? null,
      params.createdAt ?? new Date(),
    );
  }

  withProfile(params: UpdateUserProfileParams): User {
    return new User(this.id, params.email, this.passwordHash, params.name, this.createdAt);
  }

  withPassword(passwordHash: string): User {
    return new User(this.id, this.email, passwordHash, this.name, this.createdAt);
  }
}
