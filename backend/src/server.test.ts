import { beforeEach, describe, expect, it } from "vitest";
import request from "supertest";
import type { Express } from "express";
import { createServer, type ServerDependencies } from "./server";
import { RegisterUserUseCase } from "./application/use-cases/RegisterUserUseCase";
import { AuthenticateUserUseCase } from "./application/use-cases/AuthenticateUserUseCase";
import { RefreshAccessTokenUseCase } from "./application/use-cases/RefreshAccessTokenUseCase";
import { LogoutUseCase } from "./application/use-cases/LogoutUseCase";
import { SessionIssuer } from "./application/services/SessionIssuer";
import { InMemoryUserRepository } from "./adapters/out/persistence/InMemoryUserRepository";
import { InMemoryRefreshTokenRepository } from "./adapters/out/persistence/InMemoryRefreshTokenRepository";
import { BcryptPasswordHasher } from "./adapters/out/security/BcryptPasswordHasher";
import { JwtTokenService } from "./adapters/out/security/JwtTokenService";
import { CryptoIdGenerator } from "./adapters/out/security/CryptoIdGenerator";
import { RandomRefreshTokenGenerator } from "./adapters/out/security/RandomRefreshTokenGenerator";
import { SystemClock } from "./adapters/out/time/SystemClock";

const REFRESH_TOKEN_TTL_MS = 7 * 24 * 60 * 60 * 1000;

function buildTestDependencies(): ServerDependencies {
  const userRepository = new InMemoryUserRepository();
  const refreshTokenRepository = new InMemoryRefreshTokenRepository();
  const passwordHasher = new BcryptPasswordHasher();
  const tokenService = new JwtTokenService("test-secret");
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

describe("Auth HTTP API (RF-001, RF-002, RF-003, RF-008)", () => {
  let app: Express;

  beforeEach(() => {
    app = createServer(buildTestDependencies());
  });

  it("registers a new user", async () => {
    const response = await request(app).post("/auth/register").send({
      email: "user@example.com",
      password: "StrongPass1",
    });

    expect(response.status).toBe(201);
    expect(response.body.userId).toBeDefined();
  });

  it("rejects registration with a duplicate email", async () => {
    await request(app).post("/auth/register").send({ email: "user@example.com", password: "StrongPass1" });

    const response = await request(app).post("/auth/register").send({
      email: "user@example.com",
      password: "AnotherPass1",
    });

    expect(response.status).toBe(400);
  });

  it("rejects registration with a weak password", async () => {
    const response = await request(app).post("/auth/register").send({
      email: "user@example.com",
      password: "short",
    });

    expect(response.status).toBe(400);
  });

  it("rejects registration with a missing password instead of returning a 500", async () => {
    const response = await request(app).post("/auth/register").send({ email: "user@example.com" });

    expect(response.status).toBe(400);
  });

  it("rejects login with an empty request body instead of returning a 500", async () => {
    const response = await request(app).post("/auth/login").send({});

    expect(response.status).toBe(400);
  });

  it("logs in, receives an access + refresh token pair, and accesses a protected route", async () => {
    await request(app).post("/auth/register").send({ email: "user@example.com", password: "StrongPass1" });

    const loginResponse = await request(app).post("/auth/login").send({
      email: "user@example.com",
      password: "StrongPass1",
    });

    expect(loginResponse.status).toBe(200);
    expect(typeof loginResponse.body.token).toBe("string");
    expect(typeof loginResponse.body.refreshToken).toBe("string");

    const meResponse = await request(app).get("/auth/me").set("Authorization", `Bearer ${loginResponse.body.token}`);

    expect(meResponse.status).toBe(200);
    expect(meResponse.body.userId).toBeDefined();
  });

  it("rejects login with a wrong password", async () => {
    await request(app).post("/auth/register").send({ email: "user@example.com", password: "StrongPass1" });

    const response = await request(app).post("/auth/login").send({
      email: "user@example.com",
      password: "WrongPass1",
    });

    expect(response.status).toBe(401);
  });

  it("rejects access to a protected route without a token", async () => {
    const response = await request(app).get("/auth/me");

    expect(response.status).toBe(401);
  });

  it("rejects access to a protected route with a malformed token", async () => {
    const response = await request(app).get("/auth/me").set("Authorization", "Bearer not-a-real-token");

    expect(response.status).toBe(401);
  });

  describe("POST /auth/refresh (RF-008 — renovação segura)", () => {
    it("exchanges a valid refresh token for a new access + refresh token pair", async () => {
      await request(app).post("/auth/register").send({ email: "user@example.com", password: "StrongPass1" });
      const loginResponse = await request(app)
        .post("/auth/login")
        .send({ email: "user@example.com", password: "StrongPass1" });

      const refreshResponse = await request(app)
        .post("/auth/refresh")
        .send({ refreshToken: loginResponse.body.refreshToken });

      expect(refreshResponse.status).toBe(200);
      expect(typeof refreshResponse.body.token).toBe("string");
      expect(refreshResponse.body.refreshToken).not.toBe(loginResponse.body.refreshToken);

      const meResponse = await request(app)
        .get("/auth/me")
        .set("Authorization", `Bearer ${refreshResponse.body.token}`);
      expect(meResponse.status).toBe(200);
    });

    it("rejects reuse of an already-rotated refresh token", async () => {
      await request(app).post("/auth/register").send({ email: "user@example.com", password: "StrongPass1" });
      const loginResponse = await request(app)
        .post("/auth/login")
        .send({ email: "user@example.com", password: "StrongPass1" });

      await request(app).post("/auth/refresh").send({ refreshToken: loginResponse.body.refreshToken });

      const reuseResponse = await request(app)
        .post("/auth/refresh")
        .send({ refreshToken: loginResponse.body.refreshToken });

      expect(reuseResponse.status).toBe(401);
    });

    it("rejects an unknown refresh token", async () => {
      const response = await request(app).post("/auth/refresh").send({ refreshToken: "never-issued" });

      expect(response.status).toBe(401);
    });

    it("rejects a request body without a refreshToken field instead of returning a 500", async () => {
      const response = await request(app).post("/auth/refresh").send({});

      expect(response.status).toBe(400);
    });
  });

  describe("POST /auth/logout (RF-008)", () => {
    it("revokes the refresh token so it can no longer be used to refresh", async () => {
      await request(app).post("/auth/register").send({ email: "user@example.com", password: "StrongPass1" });
      const loginResponse = await request(app)
        .post("/auth/login")
        .send({ email: "user@example.com", password: "StrongPass1" });

      const logoutResponse = await request(app)
        .post("/auth/logout")
        .send({ refreshToken: loginResponse.body.refreshToken });
      expect(logoutResponse.status).toBe(204);

      const refreshResponse = await request(app)
        .post("/auth/refresh")
        .send({ refreshToken: loginResponse.body.refreshToken });
      expect(refreshResponse.status).toBe(401);
    });

    it("is idempotent and does not error for an unknown refresh token", async () => {
      const response = await request(app).post("/auth/logout").send({ refreshToken: "never-issued" });

      expect(response.status).toBe(204);
    });

    it("rejects a request body without a refreshToken field instead of returning a 500", async () => {
      const response = await request(app).post("/auth/logout").send({});

      expect(response.status).toBe(400);
    });
  });
});
