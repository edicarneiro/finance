import { InvalidRequestBodyError } from "./errors/InvalidRequestBodyError";

export interface MfaLoginConfirmationBody {
  challengeToken: string;
  code: string;
}

export function parseMfaLoginConfirmation(body: unknown): MfaLoginConfirmationBody {
  const { challengeToken, code } = (body ?? {}) as Record<string, unknown>;

  if (typeof challengeToken !== "string" || typeof code !== "string") {
    throw new InvalidRequestBodyError('Os campos "challengeToken" e "code" são obrigatórios e devem ser texto.');
  }

  return { challengeToken, code };
}
