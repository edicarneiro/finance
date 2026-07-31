import type { ErrorRequestHandler } from "express";
import { DuplicateEmailError } from "../../../domain/user/errors/DuplicateEmailError";
import { InvalidCredentialsError } from "../../../domain/user/errors/InvalidCredentialsError";
import { InvalidEmailError } from "../../../domain/user/errors/InvalidEmailError";
import { WeakPasswordError } from "../../../domain/user/errors/WeakPasswordError";
import { InvalidNameError } from "../../../domain/user/errors/InvalidNameError";
import { InvalidConsentVersionError } from "../../../domain/user/errors/InvalidConsentVersionError";
import { UserNotFoundError } from "../../../domain/user/errors/UserNotFoundError";
import { InvalidRefreshTokenError } from "../../../domain/session/errors/InvalidRefreshTokenError";
import { InvalidOrExpiredResetTokenError } from "../../../domain/user/errors/InvalidOrExpiredResetTokenError";
import { InvalidRequestBodyError } from "./errors/InvalidRequestBodyError";

const CLIENT_ERROR_TYPES = [
  DuplicateEmailError,
  InvalidEmailError,
  WeakPasswordError,
  InvalidNameError,
  InvalidConsentVersionError,
  InvalidRequestBodyError,
];
const UNAUTHORIZED_ERROR_TYPES = [InvalidCredentialsError, InvalidRefreshTokenError, InvalidOrExpiredResetTokenError];
const NOT_FOUND_ERROR_TYPES = [UserNotFoundError];

/**
 * Maps domain errors to HTTP responses without leaking internals (stack traces,
 * driver-level messages) to the client, per rules.md §4 (Segurança).
 */
export const errorHandler: ErrorRequestHandler = (error, _req, res, _next) => {
  if (UNAUTHORIZED_ERROR_TYPES.some((ErrorType) => error instanceof ErrorType)) {
    res.status(401).json({ error: error.message });
    return;
  }

  if (NOT_FOUND_ERROR_TYPES.some((ErrorType) => error instanceof ErrorType)) {
    res.status(404).json({ error: error.message });
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
