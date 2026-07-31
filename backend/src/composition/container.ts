import Database from "better-sqlite3";
import { RegisterUserUseCase } from "../application/use-cases/RegisterUserUseCase";
import { AuthenticateUserUseCase } from "../application/use-cases/AuthenticateUserUseCase";
import { RefreshAccessTokenUseCase } from "../application/use-cases/RefreshAccessTokenUseCase";
import { LogoutUseCase } from "../application/use-cases/LogoutUseCase";
import { RequestPasswordResetUseCase } from "../application/use-cases/RequestPasswordResetUseCase";
import { ResetPasswordUseCase } from "../application/use-cases/ResetPasswordUseCase";
import { GetProfileUseCase } from "../application/use-cases/GetProfileUseCase";
import { UpdateProfileUseCase } from "../application/use-cases/UpdateProfileUseCase";
import { RecordConsentUseCase } from "../application/use-cases/RecordConsentUseCase";
import { ListConsentHistoryUseCase } from "../application/use-cases/ListConsentHistoryUseCase";
import { DeleteAccountUseCase } from "../application/use-cases/DeleteAccountUseCase";
import { EnrollMfaUseCase } from "../application/use-cases/EnrollMfaUseCase";
import { ConfirmMfaEnrollmentUseCase } from "../application/use-cases/ConfirmMfaEnrollmentUseCase";
import { DisableMfaUseCase } from "../application/use-cases/DisableMfaUseCase";
import { GetMfaStatusUseCase } from "../application/use-cases/GetMfaStatusUseCase";
import { CompleteMfaLoginUseCase } from "../application/use-cases/CompleteMfaLoginUseCase";
import { SessionIssuer } from "../application/services/SessionIssuer";
import { MfaChallengeIssuer } from "../application/services/MfaChallengeIssuer";
import { SqliteUserRepository } from "../adapters/out/persistence/SqliteUserRepository";
import { SqliteRefreshTokenRepository } from "../adapters/out/persistence/SqliteRefreshTokenRepository";
import { SqliteConsentRepository } from "../adapters/out/persistence/SqliteConsentRepository";
import { SqlitePasswordResetTokenRepository } from "../adapters/out/persistence/SqlitePasswordResetTokenRepository";
import { SqliteMfaCredentialRepository } from "../adapters/out/persistence/SqliteMfaCredentialRepository";
import { SqliteMfaChallengeRepository } from "../adapters/out/persistence/SqliteMfaChallengeRepository";
import { BcryptPasswordHasher } from "../adapters/out/security/BcryptPasswordHasher";
import { JwtTokenService } from "../adapters/out/security/JwtTokenService";
import { CryptoIdGenerator } from "../adapters/out/security/CryptoIdGenerator";
import { RandomRefreshTokenGenerator } from "../adapters/out/security/RandomRefreshTokenGenerator";
import { RandomPasswordResetTokenGenerator } from "../adapters/out/security/RandomPasswordResetTokenGenerator";
import { RandomMfaChallengeGenerator } from "../adapters/out/security/RandomMfaChallengeGenerator";
import { OtplibTotpService } from "../adapters/out/security/OtplibTotpService";
import { AesSecretCipher } from "../adapters/out/security/AesSecretCipher";
import { ConsolePasswordResetNotifier } from "../adapters/out/notification/ConsolePasswordResetNotifier";
import { SystemClock } from "../adapters/out/time/SystemClock";
import type { TokenService } from "../application/ports/TokenService";

const REFRESH_TOKEN_TTL_MS = 7 * 24 * 60 * 60 * 1000; // 7 dias (ADR-0007)
const PASSWORD_RESET_TOKEN_TTL_MS = 60 * 60 * 1000; // 1 hora (ADR-0009)
const MFA_CHALLENGE_TTL_MS = 5 * 60 * 1000; // 5 minutos (ADR-0012)

export interface ContainerConfig {
  databasePath: string;
  jwtSecret: string;
  /** 32-byte key, as a Buffer (ADR-0011) — encrypts MFA secrets at rest. */
  mfaEncryptionKey: Buffer;
}

export interface Container {
  registerUser: RegisterUserUseCase;
  authenticateUser: AuthenticateUserUseCase;
  refreshAccessToken: RefreshAccessTokenUseCase;
  logout: LogoutUseCase;
  requestPasswordReset: RequestPasswordResetUseCase;
  resetPassword: ResetPasswordUseCase;
  getProfile: GetProfileUseCase;
  updateProfile: UpdateProfileUseCase;
  recordConsent: RecordConsentUseCase;
  listConsentHistory: ListConsentHistoryUseCase;
  deleteAccount: DeleteAccountUseCase;
  enrollMfa: EnrollMfaUseCase;
  confirmMfaEnrollment: ConfirmMfaEnrollmentUseCase;
  disableMfa: DisableMfaUseCase;
  getMfaStatus: GetMfaStatusUseCase;
  completeMfaLogin: CompleteMfaLoginUseCase;
  tokenService: TokenService;
}

/**
 * Composition root (ADR-0002): the only module allowed to know both the
 * application use cases and the concrete adapter implementations.
 */
export function buildContainer(config: ContainerConfig): Container {
  const database = new Database(config.databasePath);
  const userRepository = new SqliteUserRepository(database);
  const refreshTokenRepository = new SqliteRefreshTokenRepository(database);
  const consentRepository = new SqliteConsentRepository(database);
  const passwordResetTokenRepository = new SqlitePasswordResetTokenRepository(database);
  const secretCipher = new AesSecretCipher(config.mfaEncryptionKey);
  const mfaCredentialRepository = new SqliteMfaCredentialRepository(database, secretCipher);
  const mfaChallengeRepository = new SqliteMfaChallengeRepository(database);
  const passwordHasher = new BcryptPasswordHasher();
  const tokenService = new JwtTokenService(config.jwtSecret);
  const idGenerator = new CryptoIdGenerator();
  const refreshTokenGenerator = new RandomRefreshTokenGenerator();
  const passwordResetTokenGenerator = new RandomPasswordResetTokenGenerator();
  const mfaChallengeGenerator = new RandomMfaChallengeGenerator();
  const passwordResetNotifier = new ConsolePasswordResetNotifier();
  const totpService = new OtplibTotpService();
  const clock = new SystemClock();

  const sessionIssuer = new SessionIssuer(
    tokenService,
    refreshTokenRepository,
    refreshTokenGenerator,
    idGenerator,
    clock,
    REFRESH_TOKEN_TTL_MS,
  );

  const mfaChallengeIssuer = new MfaChallengeIssuer(
    mfaChallengeRepository,
    mfaChallengeGenerator,
    idGenerator,
    clock,
    MFA_CHALLENGE_TTL_MS,
  );

  return {
    registerUser: new RegisterUserUseCase(userRepository, passwordHasher, idGenerator),
    authenticateUser: new AuthenticateUserUseCase(
      userRepository,
      passwordHasher,
      sessionIssuer,
      mfaCredentialRepository,
      mfaChallengeIssuer,
    ),
    refreshAccessToken: new RefreshAccessTokenUseCase(refreshTokenRepository, sessionIssuer, clock),
    logout: new LogoutUseCase(refreshTokenRepository, clock),
    requestPasswordReset: new RequestPasswordResetUseCase(
      userRepository,
      passwordResetTokenRepository,
      passwordResetTokenGenerator,
      idGenerator,
      clock,
      passwordResetNotifier,
      PASSWORD_RESET_TOKEN_TTL_MS,
    ),
    resetPassword: new ResetPasswordUseCase(
      passwordResetTokenRepository,
      userRepository,
      passwordHasher,
      refreshTokenRepository,
      clock,
    ),
    getProfile: new GetProfileUseCase(userRepository),
    updateProfile: new UpdateProfileUseCase(userRepository),
    recordConsent: new RecordConsentUseCase(userRepository, consentRepository, idGenerator, clock),
    listConsentHistory: new ListConsentHistoryUseCase(consentRepository),
    deleteAccount: new DeleteAccountUseCase(
      userRepository,
      passwordHasher,
      refreshTokenRepository,
      passwordResetTokenRepository,
      mfaCredentialRepository,
      idGenerator,
      clock,
    ),
    enrollMfa: new EnrollMfaUseCase(userRepository, passwordHasher, mfaCredentialRepository, totpService, idGenerator, clock),
    confirmMfaEnrollment: new ConfirmMfaEnrollmentUseCase(userRepository, mfaCredentialRepository, totpService, clock),
    disableMfa: new DisableMfaUseCase(userRepository, passwordHasher, mfaCredentialRepository, clock),
    getMfaStatus: new GetMfaStatusUseCase(userRepository, mfaCredentialRepository),
    completeMfaLogin: new CompleteMfaLoginUseCase(mfaChallengeRepository, mfaCredentialRepository, totpService, sessionIssuer, clock),
    tokenService,
  };
}
