import type { PasswordResetNotification, PasswordResetNotifier } from "../application/ports/PasswordResetNotifier";

export class FakePasswordResetNotifier implements PasswordResetNotifier {
  public readonly sent: PasswordResetNotification[] = [];

  async notifyPasswordResetRequested(notification: PasswordResetNotification): Promise<void> {
    this.sent.push(notification);
  }
}
