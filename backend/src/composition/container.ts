import Database from "better-sqlite3";
import { RegisterUserUseCase } from "../application/use-cases/RegisterUserUseCase";
import { AuthenticateUserUseCase } from "../application/use-cases/AuthenticateUserUseCase";
import { RefreshAccessTokenUseCase } from "../application/use-cases/RefreshAccessTokenUseCase";
import { LogoutUseCase } from "../application/use-cases/LogoutUseCase";
import { SessionIssuer } from "../application/services/SessionIssuer";
import { SqliteUserRepository } from "../adapters/out/persistence/SqliteUserRepository";
import { SqliteRefreshTokenRepository } from "../adapters/out/persistence/SqliteRefreshTokenRepository";
import { BcryptPasswordHasher } from "../adapters/out/security/BcryptPasswordHasher";
import { JwtTokenService } from "../adapters/out/security/JwtTokenService";
import { CryptoIdGenerator } from "../adapters/out/security/CryptoIdGenerator";
import { RandomRefreshTokenGenerator } from "../adapters/out/security/RandomRefreshTokenGenerator";
import { SystemClock } from "../adapters/out/time/SystemClock";
import type { TokenService } from "../application/ports/TokenService";

const REFRESH_TOKEN_TTL_MS = 7 * 24 * 60 * 60 * 1000; // 7 dias (ADR-0007)

export interface ContainerConfig {
  databasePath: string;
  jwtSecret: string;
}

export interface Container {
  registerUser: RegisterUserUseCase;
  authenticateUser: AuthenticateUserUseCase;
  refreshAccessToken: RefreshAccessTokenUseCase;
  logout: LogoutUseCase;
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
  const passwordHasher = new BcryptPasswordHasher();
  const tokenService = new JwtTokenService(config.jwtSecret);
  const idGenerator = new CryptoIdGenerator();
  const refreshTokenGenerator = new RandomRefreshTokenGenerator();
  const clock = new SystemClock();

  const sessionIssuer = new SessionIssuer(
    tokenService,
    refreshTokenRepository,
    refreshTokenGenerator,
    idGenerator,
    clock,
    REFRESH_TOKEN_TTL_MS,
  );

  return {
    registerUser: new RegisterUserUseCase(userRepository, passwordHasher, idGenerator),
    authenticateUser: new AuthenticateUserUseCase(userRepository, passwordHasher, sessionIssuer),
    refreshAccessToken: new RefreshAccessTokenUseCase(refreshTokenRepository, sessionIssuer, clock),
    logout: new LogoutUseCase(refreshTokenRepository, clock),
    tokenService,
  };
}
