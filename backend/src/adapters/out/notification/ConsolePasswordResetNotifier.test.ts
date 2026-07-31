import { describe, expect, it, vi } from "vitest";
import { ConsolePasswordResetNotifier } from "./ConsolePasswordResetNotifier";
import { Email } from "../../../domain/user/Email";

describe("ConsolePasswordResetNotifier", () => {
  it("logs the reset token so it can be used during development (ADR-0009)", async () => {
    const logSpy = vi.spyOn(console, "log").mockImplementation(() => undefined);
    const notifier = new ConsolePasswordResetNotifier();

    await notifier.notifyPasswordResetRequested({
      email: Email.create("user@example.com"),
      rawToken: "raw-token-value",
      expiresAt: new Date("2026-01-01T01:00:00.000Z"),
    });

    expect(logSpy).toHaveBeenCalledOnce();
    const loggedMessage = logSpy.mock.calls[0].join(" ");
    expect(loggedMessage).toContain("user@example.com");
    expect(loggedMessage).toContain("raw-token-value");

    logSpy.mockRestore();
  });
});
