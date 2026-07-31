import { InvalidRequestBodyError } from "./errors/InvalidRequestBodyError";

export interface Credentials {
  email: string;
  password: string;
}

export function parseCredentials(body: unknown): Credentials {
  const { email, password } = (body ?? {}) as Record<string, unknown>;

  if (typeof email !== "string" || typeof password !== "string") {
    throw new InvalidRequestBodyError('Os campos "email" e "password" são obrigatórios e devem ser texto.');
  }

  return { email, password };
}
