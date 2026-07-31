import { Router, type NextFunction, type RequestHandler, type Response } from "express";
import { GetProfileUseCase } from "../../../application/use-cases/GetProfileUseCase";
import { UpdateProfileUseCase } from "../../../application/use-cases/UpdateProfileUseCase";
import { RecordConsentUseCase } from "../../../application/use-cases/RecordConsentUseCase";
import { ListConsentHistoryUseCase } from "../../../application/use-cases/ListConsentHistoryUseCase";
import { parseProfileUpdate } from "./parseProfileUpdate";
import { parseConsentVersion } from "./parseConsentVersion";
import { getAuthenticatedUserId, type AuthenticatedRequest } from "./requireAuth";

export interface UserRoutesDependencies {
  getProfile: GetProfileUseCase;
  updateProfile: UpdateProfileUseCase;
  recordConsent: RecordConsentUseCase;
  listConsentHistory: ListConsentHistoryUseCase;
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

  return router;
}
