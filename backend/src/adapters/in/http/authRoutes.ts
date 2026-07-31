import { Router, type NextFunction, type Request, type Response } from "express";
import { RegisterUserUseCase } from "../../../application/use-cases/RegisterUserUseCase";
import { AuthenticateUserUseCase } from "../../../application/use-cases/AuthenticateUserUseCase";
import { RefreshAccessTokenUseCase } from "../../../application/use-cases/RefreshAccessTokenUseCase";
import { LogoutUseCase } from "../../../application/use-cases/LogoutUseCase";
import { RequestPasswordResetUseCase } from "../../../application/use-cases/RequestPasswordResetUseCase";
import { ResetPasswordUseCase } from "../../../application/use-cases/ResetPasswordUseCase";
import { CompleteMfaLoginUseCase } from "../../../application/use-cases/CompleteMfaLoginUseCase";
import { parseCredentials } from "./parseCredentials";
import { parseRefreshToken } from "./parseRefreshToken";
import { parsePasswordResetRequest } from "./parsePasswordResetRequest";
import { parsePasswordResetConfirmation } from "./parsePasswordResetConfirmation";
import { parseMfaLoginConfirmation } from "./parseMfaLoginConfirmation";

export interface AuthRoutesDependencies {
  registerUser: RegisterUserUseCase;
  authenticateUser: AuthenticateUserUseCase;
  refreshAccessToken: RefreshAccessTokenUseCase;
  logout: LogoutUseCase;
  requestPasswordReset: RequestPasswordResetUseCase;
  resetPassword: ResetPasswordUseCase;
  completeMfaLogin: CompleteMfaLoginUseCase;
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
      if (result.mfaRequired) {
        res.status(200).json({ mfaRequired: true, challengeToken: result.challengeToken });
      } else {
        res.status(200).json({ mfaRequired: false, token: result.token, refreshToken: result.refreshToken });
      }
    } catch (error) {
      next(error);
    }
  });

  router.post("/login/mfa", async (req: Request, res: Response, next: NextFunction) => {
    try {
      const { challengeToken, code } = parseMfaLoginConfirmation(req.body);
      const result = await deps.completeMfaLogin.execute({ challengeToken, code });
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

  // Always 202, whether or not the email exists (ADR-0009 — anti-enumeration).
  router.post("/password-reset/request", async (req: Request, res: Response, next: NextFunction) => {
    try {
      const email = parsePasswordResetRequest(req.body);
      await deps.requestPasswordReset.execute({ email });
      res.status(202).send();
    } catch (error) {
      next(error);
    }
  });

  router.post("/password-reset/confirm", async (req: Request, res: Response, next: NextFunction) => {
    try {
      const { token, newPassword } = parsePasswordResetConfirmation(req.body);
      await deps.resetPassword.execute({ token, newPassword });
      res.status(200).send();
    } catch (error) {
      next(error);
    }
  });

  return router;
}
