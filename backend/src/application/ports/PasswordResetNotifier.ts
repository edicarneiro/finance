import { Email } from "../../domain/user/Email";

export interface PasswordResetNotification {
  email: Email;
  rawToken: string;
  expiresAt: Date;
}

/**
 * Expresses intent ("notify about a password reset request"), not
 * mechanism — the adapter decides how (email today, possibly other
 * channels later) without this port or its callers changing (ADR-0009).
 */
export interface PasswordResetNotifier {
  notifyPasswordResetRequested(notification: PasswordResetNotification): Promise<void>;
}
