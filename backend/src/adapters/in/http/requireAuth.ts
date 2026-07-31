import type { NextFunction, Request, Response } from "express";
import { TokenService } from "../../../application/ports/TokenService";

export interface AuthenticatedRequest extends Request {
  userId?: string;
}

export function createRequireAuth(tokenService: TokenService) {
  return function requireAuth(req: AuthenticatedRequest, res: Response, next: NextFunction): void {
    const header = req.headers.authorization;
    const token = header?.startsWith("Bearer ") ? header.slice("Bearer ".length) : null;

    if (!token) {
      res.status(401).json({ error: "Token de autenticação ausente." });
      return;
    }

    const payload = tokenService.verify(token);
    if (!payload) {
      res.status(401).json({ error: "Token de autenticação inválido ou expirado." });
      return;
    }

    req.userId = payload.userId;
    next();
  };
}

/**
 * Reads req.userId set by requireAuth. Throwing here signals a wiring bug
 * (a route using this without requireAuth ahead of it), not a client error.
 */
export function getAuthenticatedUserId(req: AuthenticatedRequest): string {
  if (!req.userId) {
    throw new Error("getAuthenticatedUserId called on a request that did not go through requireAuth.");
  }
  return req.userId;
}
