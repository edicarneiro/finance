import { InvalidRequestBodyError } from "./errors/InvalidRequestBodyError";

/**
 * Shared by every endpoint that requires re-entering the current password as
 * explicit confirmation for a sensitive action: account deletion (RF-007),
 * MFA enrollment and MFA disable (RF-004, ADR-0011).
 */
export function parsePasswordConfirmation(body: unknown): string {
  const { password } = (body ?? {}) as Record<string, unknown>;

  if (typeof password !== "string") {
    throw new InvalidRequestBodyError('O campo "password" é obrigatório e deve ser texto.');
  }

  return password;
}
