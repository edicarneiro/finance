import { PasswordResetNotification, PasswordResetNotifier } from "../../../application/ports/PasswordResetNotifier";

/**
 * Development-only stand-in for a real email provider (ADR-0009): logs the
 * reset link/token instead of sending an actual email. Must be replaced by
 * a real adapter before any production deployment.
 */
export class ConsolePasswordResetNotifier implements PasswordResetNotifier {
  async notifyPasswordResetRequested(notification: PasswordResetNotification): Promise<void> {
    // eslint-disable-next-line no-console
    console.log(
      `[password-reset] ${notification.email.toString()} — token: ${notification.rawToken} — expira em: ${notification.expiresAt.toISOString()}`,
    );
  }
}
