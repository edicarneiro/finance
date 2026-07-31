import { InvalidRequestBodyError } from "./errors/InvalidRequestBodyError";

export function parseConsentVersion(body: unknown): string {
  const { version } = (body ?? {}) as Record<string, unknown>;

  if (typeof version !== "string") {
    throw new InvalidRequestBodyError('O campo "version" é obrigatório e deve ser texto.');
  }

  return version;
}
