export interface BuildOtpauthUrlParams {
  secret: string;
  accountName: string;
}

export interface VerifyTotpParams {
  secret: string;
  token: string;
}

export interface TotpService {
  generateSecret(): string;
  buildOtpauthUrl(params: BuildOtpauthUrlParams): string;
  verify(params: VerifyTotpParams): Promise<boolean>;
}
