import { InvalidRequestBodyError } from "./errors/InvalidRequestBodyError";

export interface ProfileUpdateBody {
  name: string;
  email: string;
}

export function parseProfileUpdate(body: unknown): ProfileUpdateBody {
  const { name, email } = (body ?? {}) as Record<string, unknown>;

  if (typeof name !== "string" || typeof email !== "string") {
    throw new InvalidRequestBodyError('Os campos "name" e "email" são obrigatórios e devem ser texto.');
  }

  return { name, email };
}
