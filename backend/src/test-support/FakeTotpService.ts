import type { BuildOtpauthUrlParams, TotpService, VerifyTotpParams } from "../application/ports/TotpService";

/**
 * Deterministic double: the "valid" code for a secret is a fixed, predictable
 * string derived from it — avoids real, time-window-dependent TOTP codes in
 * use-case tests (rules.md §3: no dependency on real time).
 */
export class FakeTotpService implements TotpService {
  generateSecret(): string {
    return "FAKE_SECRET";
  }

  buildOtpauthUrl(params: BuildOtpauthUrlParams): string {
    return `otpauth://totp/${params.accountName}?secret=${params.secret}&issuer=FinancePulse`;
  }

  async verify(params: VerifyTotpParams): Promise<boolean> {
    return params.token === `valid-code-for-${params.secret}`;
  }
}
