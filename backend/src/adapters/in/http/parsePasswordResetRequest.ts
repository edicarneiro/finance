import { InvalidRequestBodyError } from "./errors/InvalidRequestBodyError";

export function parsePasswordResetRequest(body: unknown): string {
  const { email } = (body ?? {}) as Record<string, unknown>;

  if (typeof email !== "string") {
    throw new InvalidRequestBodyError('O campo "email" é obrigatório e deve ser texto.');
  }

  return email;
}
