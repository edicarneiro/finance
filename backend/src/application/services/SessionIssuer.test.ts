import { describe, expect, it } from "vitest";
import { SessionIssuer } from "./SessionIssuer";
import { InMemoryRefreshTokenRepository } from "../../adapters/out/persistence/InMemoryRefreshTokenRepository";
import { FakeTokenService } from "../../test-support/FakeTokenService";
import { SequentialIdGenerator } from "../../test-support/SequentialIdGenerator";
import { SequentialRefreshTokenGenerator } from "../../test-support/SequentialRefreshTokenGenerator";
import { FixedClock } from "../../test-support/FixedClock";

const REFRESH_TOKEN_TTL_MS = 7 * 24 * 60 * 60 * 1000;

describe("SessionIssuer", () => {
  it("issues an access token and a persisted refresh token for a user", async () => {
    const refreshTokenRepository = new InMemoryRefreshTokenRepository();
    const now = new Date("2026-01-01T00:00:00.000Z");
    const issuer = new SessionIssuer(
      new FakeTokenService(),
      refreshTokenRepository,
      new SequentialRefreshTokenGenerator(),
      new SequentialIdGenerator("session"),
      new FixedClock(now),
      REFRESH_TOKEN_TTL_MS,
    );

    const result = await issuer.issueFor("user-1");

    expect(result.accessToken).toBe("token-for-user-1");
    expect(result.refreshToken).toBe("refresh-token-1");

    const stored = await refreshTokenRepository.findByRawToken("refresh-token-1");
    expect(stored?.userId).toBe("user-1");
    expect(stored?.isValid(now)).toBe(true);
    expect(stored?.expiresAt.getTime()).toBe(now.getTime() + REFRESH_TOKEN_TTL_MS);
  });

  it("issues a different refresh token on each call", async () => {
    const refreshTokenRepository = new InMemoryRefreshTokenRepository();
    const issuer = new SessionIssuer(
      new FakeTokenService(),
      refreshTokenRepository,
      new SequentialRefreshTokenGenerator(),
      new SequentialIdGenerator("session"),
      new FixedClock(new Date("2026-01-01T00:00:00.000Z")),
      REFRESH_TOKEN_TTL_MS,
    );

    const first = await issuer.issueFor("user-1");
    const second = await issuer.issueFor("user-1");

    expect(first.refreshToken).not.toBe(second.refreshToken);
  });
});
