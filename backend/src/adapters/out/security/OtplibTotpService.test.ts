import { describe, expect, it } from "vitest";
import { generate } from "otplib";
import { OtplibTotpService } from "./OtplibTotpService";

describe("OtplibTotpService", () => {
  it("generates a base32 secret", () => {
    const service = new OtplibTotpService();

    const secret = service.generateSecret();

    expect(secret).toMatch(/^[A-Z2-7]+$/);
    expect(secret.length).toBeGreaterThan(0);
  });

  it("builds an otpauth:// URI containing the account name and secret", () => {
    const service = new OtplibTotpService();

    const url = service.buildOtpauthUrl({ secret: "JBSWY3DPEHPK3PXP", accountName: "user@example.com" });

    expect(url).toMatch(/^otpauth:\/\/totp\//);
    expect(url).toContain("user%40example.com");
    expect(url).toContain("secret=JBSWY3DPEHPK3PXP");
    expect(url).toContain("FinancePulse");
  });

  it("verifies a code actually generated for the same secret (real RFC 6238 round-trip)", async () => {
    const service = new OtplibTotpService();
    const secret = service.generateSecret();
    const validCode = await generate({ secret });

    await expect(service.verify({ secret, token: validCode })).resolves.toBe(true);
  });

  it("rejects an incorrect code", async () => {
    const service = new OtplibTotpService();
    const secret = service.generateSecret();

    await expect(service.verify({ secret, token: "000000" })).resolves.toBe(false);
  });
});
