import { beforeEach, describe, expect, it } from "vitest";
import { LogoutUseCase } from "./LogoutUseCase";
import { SessionIssuer } from "../services/SessionIssuer";
import { InMemoryRefreshTokenRepository } from "../../adapters/out/persistence/InMemoryRefreshTokenRepository";
import { FakeTokenService } from "../../test-support/FakeTokenService";
import { SequentialIdGenerator } from "../../test-support/SequentialIdGenerator";
import { SequentialRefreshTokenGenerator } from "../../test-support/SequentialRefreshTokenGenerator";
import { FixedClock } from "../../test-support/FixedClock";

const REFRESH_TOKEN_TTL_MS = 7 * 24 * 60 * 60 * 1000;

describe("LogoutUseCase", () => {
  let refreshTokenRepository: InMemoryRefreshTokenRepository;
  let sessionIssuer: SessionIssuer;
  let useCase: LogoutUseCase;

  beforeEach(() => {
    refreshTokenRepository = new InMemoryRefreshTokenRepository();
    sessionIssuer = new SessionIssuer(
      new FakeTokenService(),
      refreshTokenRepository,
      new SequentialRefreshTokenGenerator(),
      new SequentialIdGenerator("session"),
      new FixedClock(new Date("2026-01-01T00:00:00.000Z")),
      REFRESH_TOKEN_TTL_MS,
    );
    useCase = new LogoutUseCase(refreshTokenRepository, new FixedClock(new Date("2026-01-01T00:00:00.000Z")));
  });

  it("revokes the presented refresh token", async () => {
    const { refreshToken } = await sessionIssuer.issueFor("user-1");

    await useCase.execute({ refreshToken });

    const stored = await refreshTokenRepository.findByRawToken(refreshToken);
    expect(stored?.isRevoked()).toBe(true);
  });

  it("is idempotent: logging out twice with the same token does not throw", async () => {
    const { refreshToken } = await sessionIssuer.issueFor("user-1");

    await useCase.execute({ refreshToken });

    await expect(useCase.execute({ refreshToken })).resolves.toBeUndefined();
  });

  it("does not throw for an unknown refresh token", async () => {
    await expect(useCase.execute({ refreshToken: "never-issued" })).resolves.toBeUndefined();
  });
});
