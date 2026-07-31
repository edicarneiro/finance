import { InvalidRequestBodyError } from "./errors/InvalidRequestBodyError";

export function parseMfaCode(body: unknown): string {
  const { code } = (body ?? {}) as Record<string, unknown>;

  if (typeof code !== "string") {
    throw new InvalidRequestBodyError('O campo "code" é obrigatório e deve ser texto.');
  }

  return code;
}
