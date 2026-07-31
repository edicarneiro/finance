import { Router, type NextFunction, type Request, type RequestHandler, type Response } from "express";
import { RegisterUserUseCase } from "../../../application/use-cases/RegisterUserUseCase";
import { AuthenticateUserUseCase } from "../../../application/use-cases/AuthenticateUserUseCase";
import { RefreshAccessTokenUseCase } from "../../../application/use-cases/RefreshAccessTokenUseCase";
import { LogoutUseCase } from "../../../application/use-cases/LogoutUseCase";
import { parseCredentials } from "./parseCredentials";
import { parseRefreshToken } from "./parseRefreshToken";
import type { AuthenticatedRequest } from "./requireAuth";

export interface AuthRoutesDependencies {
  registerUser: RegisterUserUseCase;
  authenticateUser: AuthenticateUserUseCase;
  refreshAccessToken: RefreshAccessTokenUseCase;
  logout: LogoutUseCase;
  requireAuth: RequestHandler;
}

export function createAuthRouter(deps: AuthRoutesDependencies): Router {
  const router = Router();

  router.post("/register", async (req: Request, res: Response, next: NextFunction) => {
    try {
      const { email, password } = parseCredentials(req.body);
      const result = await deps.registerUser.execute({ email, password });
      res.status(201).json({ userId: result.userId });
    } catch (error) {
      next(error);
    }
  });

  router.post("/login", async (req: Request, res: Response, next: NextFunction) => {
    try {
      const { email, password } = parseCredentials(req.body);
      const result = await deps.authenticateUser.execute({ email, password });
      res.status(200).json({ token: result.token, refreshToken: result.refreshToken });
    } catch (error) {
      next(error);
    }
  });

  router.post("/refresh", async (req: Request, res: Response, next: NextFunction) => {
    try {
      const refreshToken = parseRefreshToken(req.body);
      const result = await deps.refreshAccessToken.execute({ refreshToken });
      res.status(200).json({ token: result.token, refreshToken: result.refreshToken });
    } catch (error) {
      next(error);
    }
  });

  router.post("/logout", async (req: Request, res: Response, next: NextFunction) => {
    try {
      const refreshToken = parseRefreshToken(req.body);
      await deps.logout.execute({ refreshToken });
      res.status(204).send();
    } catch (error) {
      next(error);
    }
  });

  // Minimal protected route proving token issuance/validation end-to-end (RF-008).
  router.get("/me", deps.requireAuth, (req: AuthenticatedRequest, res: Response) => {
    res.status(200).json({ userId: req.userId });
  });

  return router;
}
