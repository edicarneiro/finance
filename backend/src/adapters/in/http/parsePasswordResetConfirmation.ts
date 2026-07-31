import { InvalidRequestBodyError } from "./errors/InvalidRequestBodyError";

export interface PasswordResetConfirmationBody {
  token: string;
  newPassword: string;
}

export function parsePasswordResetConfirmation(body: unknown): PasswordResetConfirmationBody {
  const { token, newPassword } = (body ?? {}) as Record<string, unknown>;

  if (typeof token !== "string" || typeof newPassword !== "string") {
    throw new InvalidRequestBodyError('Os campos "token" e "newPassword" são obrigatórios e devem ser texto.');
  }

  return { token, newPassword };
}
