import { beforeEach, describe, expect, it } from "vitest";
import { RefreshAccessTokenUseCase } from "./RefreshAccessTokenUseCase";
import { SessionIssuer } from "../services/SessionIssuer";
import { InMemoryRefreshTokenRepository } from "../../adapters/out/persistence/InMemoryRefreshTokenRepository";
import { InvalidRefreshTokenError } from "../../domain/session/errors/InvalidRefreshTokenError";
import { FakeTokenService } from "../../test-support/FakeTokenService";
import { SequentialIdGenerator } from "../../test-support/SequentialIdGenerator";
import { SequentialRefreshTokenGenerator } from "../../test-support/SequentialRefreshTokenGenerator";
import { FixedClock } from "../../test-support/FixedClock";

const REFRESH_TOKEN_TTL_MS = 7 * 24 * 60 * 60 * 1000;
const ISSUED_AT = new Date("2026-01-01T00:00:00.000Z");

describe("RefreshAccessTokenUseCase", () => {
  let refreshTokenRepository: InMemoryRefreshTokenRepository;
  let clock: FixedClock;
  let sessionIssuer: SessionIssuer;
  let useCase: RefreshAccessTokenUseCase;

  beforeEach(() => {
    refreshTokenRepository = new InMemoryRefreshTokenRepository();
    clock = new FixedClock(ISSUED_AT);
    sessionIssuer = new SessionIssuer(
      new FakeTokenService(),
      refreshTokenRepository,
      new SequentialRefreshTokenGenerator(),
      new SequentialIdGenerator("session"),
      clock,
      REFRESH_TOKEN_TTL_MS,
    );
    useCase = new RefreshAccessTokenUseCase(refreshTokenRepository, sessionIssuer, clock);
  });

  it("issues a new token pair and rotates (revokes) the previous refresh token", async () => {
    const { refreshToken: originalRefreshToken } = await sessionIssuer.issueFor("user-1");

    const result = await useCase.execute({ refreshToken: originalRefreshToken });

    expect(result.token).toBe("token-for-user-1");
    expect(result.refreshToken).not.toBe(originalRefreshToken);

    const original = await refreshTokenRepository.findByRawToken(originalRefreshToken);
    expect(original?.isRevoked()).toBe(true);
  });

  it("rejects an unknown refresh token", async () => {
    await expect(useCase.execute({ refreshToken: "never-issued" })).rejects.toThrow(InvalidRefreshTokenError);
  });

  it("rejects an expired refresh token", async () => {
    const { refreshToken } = await sessionIssuer.issueFor("user-1");
    clock.advanceBy(REFRESH_TOKEN_TTL_MS + 1);

    await expect(useCase.execute({ refreshToken })).rejects.toThrow(InvalidRefreshTokenError);
  });

  it("revokes every session of the user when a reused (already-revoked) token is presented", async () => {
    const first = await sessionIssuer.issueFor("user-1");
    const second = await sessionIssuer.issueFor("user-1");

    // Legitimate rotation of the first token.
    await useCase.execute({ refreshToken: first.refreshToken });

    // The already-rotated token is presented again — treated as a theft signal.
    await expect(useCase.execute({ refreshToken: first.refreshToken })).rejects.toThrow(InvalidRefreshTokenError);

    const secondStillValid = await refreshTokenRepository.findByRawToken(second.refreshToken);
    expect(secondStillValid?.isRevoked()).toBe(true);
  });
});
