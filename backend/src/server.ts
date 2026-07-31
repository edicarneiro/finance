import express, { type Express } from "express";
import { createAuthRouter } from "./adapters/in/http/authRoutes";
import { createRequireAuth } from "./adapters/in/http/requireAuth";
import { errorHandler } from "./adapters/in/http/errorHandler";
import type { RegisterUserUseCase } from "./application/use-cases/RegisterUserUseCase";
import type { AuthenticateUserUseCase } from "./application/use-cases/AuthenticateUserUseCase";
import type { TokenService } from "./application/ports/TokenService";

export interface ServerDependencies {
  registerUser: RegisterUserUseCase;
  authenticateUser: AuthenticateUserUseCase;
  tokenService: TokenService;
}

export function createServer(deps: ServerDependencies): Express {
  const app = express();
  app.use(express.json());

  app.use(
    "/auth",
    createAuthRouter({
      registerUser: deps.registerUser,
      authenticateUser: deps.authenticateUser,
      requireAuth: createRequireAuth(deps.tokenService),
    }),
  );

  app.use(errorHandler);

  return app;
}
