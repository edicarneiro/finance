import { describe, expect, it } from "vitest";
import { MfaCredential } from "./MfaCredential";

const CREATED_AT = new Date("2026-01-01T00:00:00.000Z");

describe("MfaCredential", () => {
  it("is neither confirmed nor active right after being enrolled", () => {
    const credential = MfaCredential.enroll({ id: "mfa-1", userId: "user-1", secret: "SECRET", createdAt: CREATED_AT });

    expect(credential.isConfirmed()).toBe(false);
    expect(credential.isActive()).toBe(false);
    expect(credential.isDisabled()).toBe(false);
  });

  it("becomes active once confirmed", () => {
    const credential = MfaCredential.enroll({ id: "mfa-1", userId: "user-1", secret: "SECRET", createdAt: CREATED_AT });

    credential.confirm(CREATED_AT);

    expect(credential.isConfirmed()).toBe(true);
    expect(credential.isActive()).toBe(true);
  });

  it("is no longer active once disabled, even if previously confirmed", () => {
    const credential = MfaCredential.enroll({ id: "mfa-1", userId: "user-1", secret: "SECRET", createdAt: CREATED_AT });
    credential.confirm(CREATED_AT);

    credential.disable(CREATED_AT);

    expect(credential.isDisabled()).toBe(true);
    expect(credential.isActive()).toBe(false);
    // Disabling does not erase the historical fact that it was once confirmed.
    expect(credential.isConfirmed()).toBe(true);
  });

  it("restores a previously confirmed and active credential from persisted data", () => {
    const credential = MfaCredential.restore({
      id: "mfa-1",
      userId: "user-1",
      secret: "SECRET",
      createdAt: CREATED_AT,
      confirmedAt: CREATED_AT,
      disabledAt: null,
    });

    expect(credential.isActive()).toBe(true);
  });
});
