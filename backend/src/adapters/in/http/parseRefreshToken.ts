import { InvalidRequestBodyError } from "./errors/InvalidRequestBodyError";

export function parseRefreshToken(body: unknown): string {
  const { refreshToken } = (body ?? {}) as Record<string, unknown>;

  if (typeof refreshToken !== "string") {
    throw new InvalidRequestBodyError('O campo "refreshToken" é obrigatório e deve ser texto.');
  }

  return refreshToken;
}
