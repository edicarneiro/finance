import { generateSecret, generateURI, verify } from "otplib";
import { BuildOtpauthUrlParams, TotpService, VerifyTotpParams } from "../../../application/ports/TotpService";

const ISSUER = "FinancePulse Engine";

export class OtplibTotpService implements TotpService {
  generateSecret(): string {
    return generateSecret();
  }

  buildOtpauthUrl(params: BuildOtpauthUrlParams): string {
    return generateURI({ issuer: ISSUER, label: params.accountName, secret: params.secret });
  }

  async verify(params: VerifyTotpParams): Promise<boolean> {
    const result = await verify({ secret: params.secret, token: params.token });
    return result.valid;
  }
}
