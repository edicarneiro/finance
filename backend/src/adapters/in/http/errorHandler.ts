import type { ErrorRequestHandler } from "express";
import { DuplicateEmailError } from "../../../domain/user/errors/DuplicateEmailError";
import { InvalidCredentialsError } from "../../../domain/user/errors/InvalidCredentialsError";
import { InvalidEmailError } from "../../../domain/user/errors/InvalidEmailError";
import { WeakPasswordError } from "../../../domain/user/errors/WeakPasswordError";
import { InvalidRefreshTokenError } from "../../../domain/session/errors/InvalidRefreshTokenError";
import { InvalidRequestBodyError } from "./errors/InvalidRequestBodyError";

const CLIENT_ERROR_TYPES = [DuplicateEmailError, InvalidEmailError, WeakPasswordError, InvalidRequestBodyError];
const UNAUTHORIZED_ERROR_TYPES = [InvalidCredentialsError, InvalidRefreshTokenError];

/**
 * Maps domain errors to HTTP responses without leaking internals (stack traces,
 * driver-level messages) to the client, per rules.md §4 (Segurança).
 */
export const errorHandler: ErrorRequestHandler = (error, _req, res, _next) => {
  if (UNAUTHORIZED_ERROR_TYPES.some((ErrorType) => error instanceof ErrorType)) {
    res.status(401).json({ error: error.message });
    return;
  }

  if (CLIENT_ERROR_TYPES.some((ErrorType) => error instanceof ErrorType)) {
    res.status(400).json({ error: error.message });
    return;
  }

  // eslint-disable-next-line no-console
  console.error("Unhandled error", error);
  res.status(500).json({ error: "Erro interno inesperado." });
};
