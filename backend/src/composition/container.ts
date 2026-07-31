import Database from "better-sqlite3";
import { RegisterUserUseCase } from "../application/use-cases/RegisterUserUseCase";
import { AuthenticateUserUseCase } from "../application/use-cases/AuthenticateUserUseCase";
import { SqliteUserRepository } from "../adapters/out/persistence/SqliteUserRepository";
import { BcryptPasswordHasher } from "../adapters/out/security/BcryptPasswordHasher";
import { JwtTokenService } from "../adapters/out/security/JwtTokenService";
import { CryptoIdGenerator } from "../adapters/out/security/CryptoIdGenerator";
import type { TokenService } from "../application/ports/TokenService";

export interface ContainerConfig {
  databasePath: string;
  jwtSecret: string;
}

export interface Container {
  registerUser: RegisterUserUseCase;
  authenticateUser: AuthenticateUserUseCase;
  tokenService: TokenService;
}

/**
 * Composition root (ADR-0002): the only module allowed to know both the
 * application use cases and the concrete adapter implementations.
 */
export function buildContainer(config: ContainerConfig): Container {
  const database = new Database(config.databasePath);
  const userRepository = new SqliteUserRepository(database);
  const passwordHasher = new BcryptPasswordHasher();
  const tokenService = new JwtTokenService(config.jwtSecret);
  const idGenerator = new CryptoIdGenerator();

  return {
    registerUser: new RegisterUserUseCase(userRepository, passwordHasher, idGenerator),
    authenticateUser: new AuthenticateUserUseCase(userRepository, passwordHasher, tokenService),
    tokenService,
  };
}
