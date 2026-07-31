import { InvalidOrExpiredMfaChallengeError } from "../../domain/user/errors/InvalidOrExpiredMfaChallengeError";
import { InvalidMfaCodeError } from "../../domain/user/errors/InvalidMfaCodeError";
import { MfaChallengeRepository } from "../ports/MfaChallengeRepository";
import { MfaCredentialRepository } from "../ports/MfaCredentialRepository";
import { TotpService } from "../ports/TotpService";
import { Clock } from "../ports/Clock";
import { SessionIssuer } from "../services/SessionIssuer";

export interface CompleteMfaLoginInput {
  challengeToken: string;
  code: string;
}

export interface CompleteMfaLoginOutput {
  token: string;
  refreshToken: string;
}

export class CompleteMfaLoginUseCase {
  constructor(
    private readonly mfaChallengeRepository: MfaChallengeRepository,
    private readonly mfaCredentialRepository: MfaCredentialRepository,
    private readonly totpService: TotpService,
    private readonly sessionIssuer: SessionIssuer,
    private readonly clock: Clock,
  ) {}

  async execute(input: CompleteMfaLoginInput): Promise<CompleteMfaLoginOutput> {
    const now = this.clock.now();
    const challenge = await this.mfaChallengeRepository.findByRawToken(input.challengeToken);
    if (!challenge || !challenge.isValid(now)) {
      throw new InvalidOrExpiredMfaChallengeError();
    }

    const credential = await this.mfaCredentialRepository.findByUserId(challenge.userId);
    if (!credential || !credential.isActive()) {
      // Defensive: a challenge is only ever issued for a user with active MFA (ADR-0012).
      throw new InvalidOrExpiredMfaChallengeError();
    }

    const codeIsValid = await this.totpService.verify({ secret: credential.secret, token: input.code });
    if (!codeIsValid) {
      throw new InvalidMfaCodeError();
    }

    await this.mfaChallengeRepository.markUsed(challenge.id, now);

    const session = await this.sessionIssuer.issueFor(challenge.userId);
    return { token: session.accessToken, refreshToken: session.refreshToken };
  }
}
