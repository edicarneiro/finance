import { describe, expect, it } from "vitest";
import { ConsentRecord } from "./ConsentRecord";

describe("ConsentRecord", () => {
  it("creates an immutable record of a given consent version being accepted", () => {
    const acceptedAt = new Date("2026-01-01T00:00:00.000Z");

    const record = ConsentRecord.give({
      id: "consent-1",
      userId: "user-1",
      version: "privacy-policy-v1",
      acceptedAt,
    });

    expect(record.id).toBe("consent-1");
    expect(record.userId).toBe("user-1");
    expect(record.version).toBe("privacy-policy-v1");
    expect(record.acceptedAt).toBe(acceptedAt);
  });

  it("defaults acceptedAt to now when not provided", () => {
    const record = ConsentRecord.give({ id: "consent-1", userId: "user-1", version: "privacy-policy-v1" });

    expect(record.acceptedAt).toBeInstanceOf(Date);
  });
});
