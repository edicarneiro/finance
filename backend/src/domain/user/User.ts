import { Email } from "./Email";

export interface RegisterUserParams {
  id: string;
  email: Email;
  passwordHash: string;
  name?: string | null;
  createdAt?: Date;
  deletedAt?: Date | null;
}

export interface UpdateUserProfileParams {
  name: string;
  email: Email;
}

export interface AnonymizeUserParams {
  email: Email;
  passwordHash: string;
  deletedAt: Date;
}

export class User {
  private constructor(
    public readonly id: string,
    public readonly email: Email,
    public readonly passwordHash: string,
    public readonly name: string | null,
    public readonly createdAt: Date,
    public readonly deletedAt: Date | null,
  ) {}

  static register(params: RegisterUserParams): User {
    return new User(
      params.id,
      params.email,
      params.passwordHash,
      params.name ?? null,
      params.createdAt ?? new Date(),
      params.deletedAt ?? null,
    );
  }

  withProfile(params: UpdateUserProfileParams): User {
    return new User(this.id, params.email, this.passwordHash, params.name, this.createdAt, this.deletedAt);
  }

  withPassword(passwordHash: string): User {
    return new User(this.id, this.email, passwordHash, this.name, this.createdAt, this.deletedAt);
  }

  /**
   * Anonymizes personal data in place of a hard delete (RF-007, ADR-0010):
   * the row/id is preserved (retention obligations are an open legal
   * question per vision.md §17.1.7), but no identifying data remains.
   */
  anonymize(params: AnonymizeUserParams): User {
    return new User(this.id, params.email, params.passwordHash, null, this.createdAt, params.deletedAt);
  }

  isDeleted(): boolean {
    return this.deletedAt !== null;
  }
}
