import { Router, type NextFunction, type RequestHandler, type Response } from "express";
import { GetProfileUseCase } from "../../../application/use-cases/GetProfileUseCase";
import { UpdateProfileUseCase } from "../../../application/use-cases/UpdateProfileUseCase";
import { RecordConsentUseCase } from "../../../application/use-cases/RecordConsentUseCase";
import { ListConsentHistoryUseCase } from "../../../application/use-cases/ListConsentHistoryUseCase";
import { DeleteAccountUseCase } from "../../../application/use-cases/DeleteAccountUseCase";
import { EnrollMfaUseCase } from "../../../application/use-cases/EnrollMfaUseCase";
import { ConfirmMfaEnrollmentUseCase } from "../../../application/use-cases/ConfirmMfaEnrollmentUseCase";
import { DisableMfaUseCase } from "../../../application/use-cases/DisableMfaUseCase";
import { GetMfaStatusUseCase } from "../../../application/use-cases/GetMfaStatusUseCase";
import { parseProfileUpdate } from "./parseProfileUpdate";
import { parseConsentVersion } from "./parseConsentVersion";
import { parsePasswordConfirmation } from "./parsePasswordConfirmation";
import { parseMfaCode } from "./parseMfaCode";
import { getAuthenticatedUserId, type AuthenticatedRequest } from "./requireAuth";

export interface UserRoutesDependencies {
  getProfile: GetProfileUseCase;
  updateProfile: UpdateProfileUseCase;
  recordConsent: RecordConsentUseCase;
  listConsentHistory: ListConsentHistoryUseCase;
  deleteAccount: DeleteAccountUseCase;
  enrollMfa: EnrollMfaUseCase;
  confirmMfaEnrollment: ConfirmMfaEnrollmentUseCase;
  disableMfa: DisableMfaUseCase;
  getMfaStatus: GetMfaStatusUseCase;
  requireAuth: RequestHandler;
}

export function createUserRouter(deps: UserRoutesDependencies): Router {
  const router = Router();
  router.use(deps.requireAuth);

  router.get("/me", async (req: AuthenticatedRequest, res: Response, next: NextFunction) => {
    try {
      const profile = await deps.getProfile.execute({ userId: getAuthenticatedUserId(req) });
      res.status(200).json(profile);
    } catch (error) {
      next(error);
    }
  });

  router.put("/me", async (req: AuthenticatedRequest, res: Response, next: NextFunction) => {
    try {
      const { name, email } = parseProfileUpdate(req.body);
      const profile = await deps.updateProfile.execute({ userId: getAuthenticatedUserId(req), name, email });
      res.status(200).json(profile);
    } catch (error) {
      next(error);
    }
  });

  router.post("/me/consent", async (req: AuthenticatedRequest, res: Response, next: NextFunction) => {
    try {
      const version = parseConsentVersion(req.body);
      const record = await deps.recordConsent.execute({ userId: getAuthenticatedUserId(req), version });
      res.status(201).json(record);
    } catch (error) {
      next(error);
    }
  });

  router.get("/me/consent", async (req: AuthenticatedRequest, res: Response, next: NextFunction) => {
    try {
      const history = await deps.listConsentHistory.execute({ userId: getAuthenticatedUserId(req) });
      res.status(200).json(history);
    } catch (error) {
      next(error);
    }
  });

  router.delete("/me", async (req: AuthenticatedRequest, res: Response, next: NextFunction) => {
    try {
      const password = parsePasswordConfirmation(req.body);
      await deps.deleteAccount.execute({ userId: getAuthenticatedUserId(req), password });
      res.status(204).send();
    } catch (error) {
      next(error);
    }
  });

  router.get("/me/mfa", async (req: AuthenticatedRequest, res: Response, next: NextFunction) => {
    try {
      const status = await deps.getMfaStatus.execute({ userId: getAuthenticatedUserId(req) });
      res.status(200).json(status);
    } catch (error) {
      next(error);
    }
  });

  router.post("/me/mfa/enroll", async (req: AuthenticatedRequest, res: Response, next: NextFunction) => {
    try {
      const password = parsePasswordConfirmation(req.body);
      const result = await deps.enrollMfa.execute({ userId: getAuthenticatedUserId(req), password });
      res.status(201).json(result);
    } catch (error) {
      next(error);
    }
  });

  router.post("/me/mfa/confirm", async (req: AuthenticatedRequest, res: Response, next: NextFunction) => {
    try {
      const code = parseMfaCode(req.body);
      await deps.confirmMfaEnrollment.execute({ userId: getAuthenticatedUserId(req), code });
      res.status(200).send();
    } catch (error) {
      next(error);
    }
  });

  router.post("/me/mfa/disable", async (req: AuthenticatedRequest, res: Response, next: NextFunction) => {
    try {
      const password = parsePasswordConfirmation(req.body);
      await deps.disableMfa.execute({ userId: getAuthenticatedUserId(req), password });
      res.status(204).send();
    } catch (error) {
      next(error);
    }
  });

  return router;
}
