import { Router, type NextFunction, type Request, type RequestHandler, type Response } from "express";
import { RegisterUserUseCase } from "../../../application/use-cases/RegisterUserUseCase";
import { AuthenticateUserUseCase } from "../../../application/use-cases/AuthenticateUserUseCase";
import { parseCredentials } from "./parseCredentials";
import type { AuthenticatedRequest } from "./requireAuth";

export interface AuthRoutesDependencies {
  registerUser: RegisterUserUseCase;
  authenticateUser: AuthenticateUserUseCase;
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
      res.status(200).json({ token: result.token });
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
