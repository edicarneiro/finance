import express, { type Express } from "express";
import { createAuthRouter } from "./adapters/in/http/authRoutes";
import { createUserRouter } from "./adapters/in/http/userRoutes";
import { createRequireAuth } from "./adapters/in/http/requireAuth";
import { errorHandler } from "./adapters/in/http/errorHandler";
import type { RegisterUserUseCase } from "./application/use-cases/RegisterUserUseCase";
import type { AuthenticateUserUseCase } from "./application/use-cases/AuthenticateUserUseCase";
import type { RefreshAccessTokenUseCase } from "./application/use-cases/RefreshAccessTokenUseCase";
import type { LogoutUseCase } from "./application/use-cases/LogoutUseCase";
import type { RequestPasswordResetUseCase } from "./application/use-cases/RequestPasswordResetUseCase";
import type { ResetPasswordUseCase } from "./application/use-cases/ResetPasswordUseCase";
import type { GetProfileUseCase } from "./application/use-cases/GetProfileUseCase";
import type { UpdateProfileUseCase } from "./application/use-cases/UpdateProfileUseCase";
import type { RecordConsentUseCase } from "./application/use-cases/RecordConsentUseCase";
import type { ListConsentHistoryUseCase } from "./application/use-cases/ListConsentHistoryUseCase";
import type { DeleteAccountUseCase } from "./application/use-cases/DeleteAccountUseCase";
import type { EnrollMfaUseCase } from "./application/use-cases/EnrollMfaUseCase";
import type { ConfirmMfaEnrollmentUseCase } from "./application/use-cases/ConfirmMfaEnrollmentUseCase";
import type { DisableMfaUseCase } from "./application/use-cases/DisableMfaUseCase";
import type { GetMfaStatusUseCase } from "./application/use-cases/GetMfaStatusUseCase";
import type { CompleteMfaLoginUseCase } from "./application/use-cases/CompleteMfaLoginUseCase";
import type { TokenService } from "./application/ports/TokenService";

export interface ServerDependencies {
  registerUser: RegisterUserUseCase;
  authenticateUser: AuthenticateUserUseCase;
  refreshAccessToken: RefreshAccessTokenUseCase;
  logout: LogoutUseCase;
  requestPasswordReset: RequestPasswordResetUseCase;
  resetPassword: ResetPasswordUseCase;
  getProfile: GetProfileUseCase;
  updateProfile: UpdateProfileUseCase;
  recordConsent: RecordConsentUseCase;
  listConsentHistory: ListConsentHistoryUseCase;
  deleteAccount: DeleteAccountUseCase;
  enrollMfa: EnrollMfaUseCase;
  confirmMfaEnrollment: ConfirmMfaEnrollmentUseCase;
  disableMfa: DisableMfaUseCase;
  getMfaStatus: GetMfaStatusUseCase;
  completeMfaLogin: CompleteMfaLoginUseCase;
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
      refreshAccessToken: deps.refreshAccessToken,
      logout: deps.logout,
      requestPasswordReset: deps.requestPasswordReset,
      resetPassword: deps.resetPassword,
      completeMfaLogin: deps.completeMfaLogin,
    }),
  );

  app.use(
    "/users",
    createUserRouter({
      getProfile: deps.getProfile,
      updateProfile: deps.updateProfile,
      recordConsent: deps.recordConsent,
      listConsentHistory: deps.listConsentHistory,
      deleteAccount: deps.deleteAccount,
      enrollMfa: deps.enrollMfa,
      confirmMfaEnrollment: deps.confirmMfaEnrollment,
      disableMfa: deps.disableMfa,
      getMfaStatus: deps.getMfaStatus,
      requireAuth: createRequireAuth(deps.tokenService),
    }),
  );

  app.use(errorHandler);

  return app;
}
