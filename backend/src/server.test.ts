import { beforeEach, describe, expect, it } from "vitest";
import request from "supertest";
import type { Express } from "express";
import { createServer, type ServerDependencies } from "./server";
import { RegisterUserUseCase } from "./application/use-cases/RegisterUserUseCase";
import { AuthenticateUserUseCase } from "./application/use-cases/AuthenticateUserUseCase";
import { RefreshAccessTokenUseCase } from "./application/use-cases/RefreshAccessTokenUseCase";
import { LogoutUseCase } from "./application/use-cases/LogoutUseCase";
import { RequestPasswordResetUseCase } from "./application/use-cases/RequestPasswordResetUseCase";
import { ResetPasswordUseCase } from "./application/use-cases/ResetPasswordUseCase";
import { GetProfileUseCase } from "./application/use-cases/GetProfileUseCase";
import { UpdateProfileUseCase } from "./application/use-cases/UpdateProfileUseCase";
import { RecordConsentUseCase } from "./application/use-cases/RecordConsentUseCase";
import { ListConsentHistoryUseCase } from "./application/use-cases/ListConsentHistoryUseCase";
import { SessionIssuer } from "./application/services/SessionIssuer";
import { InMemoryUserRepository } from "./adapters/out/persistence/InMemoryUserRepository";
import { InMemoryRefreshTokenRepository } from "./adapters/out/persistence/InMemoryRefreshTokenRepository";
import { InMemoryConsentRepository } from "./adapters/out/persistence/InMemoryConsentRepository";
import { InMemoryPasswordResetTokenRepository } from "./adapters/out/persistence/InMemoryPasswordResetTokenRepository";
import { BcryptPasswordHasher } from "./adapters/out/security/BcryptPasswordHasher";
import { JwtTokenService } from "./adapters/out/security/JwtTokenService";
import { CryptoIdGenerator } from "./adapters/out/security/CryptoIdGenerator";
import { RandomRefreshTokenGenerator } from "./adapters/out/security/RandomRefreshTokenGenerator";
import { RandomPasswordResetTokenGenerator } from "./adapters/out/security/RandomPasswordResetTokenGenerator";
import { SystemClock } from "./adapters/out/time/SystemClock";
import { FakePasswordResetNotifier } from "./test-support/FakePasswordResetNotifier";

const REFRESH_TOKEN_TTL_MS = 7 * 24 * 60 * 60 * 1000;
const PASSWORD_RESET_TOKEN_TTL_MS = 60 * 60 * 1000;

function buildTestContext(): { dependencies: ServerDependencies; passwordResetNotifier: FakePasswordResetNotifier } {
  const userRepository = new InMemoryUserRepository();
  const refreshTokenRepository = new InMemoryRefreshTokenRepository();
  const consentRepository = new InMemoryConsentRepository();
  const passwordResetTokenRepository = new InMemoryPasswordResetTokenRepository();
  const passwordHasher = new BcryptPasswordHasher();
  const tokenService = new JwtTokenService("test-secret");
  const idGenerator = new CryptoIdGenerator();
  const refreshTokenGenerator = new RandomRefreshTokenGenerator();
  const passwordResetTokenGenerator = new RandomPasswordResetTokenGenerator();
  const passwordResetNotifier = new FakePasswordResetNotifier();
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
    dependencies: {
      registerUser: new RegisterUserUseCase(userRepository, passwordHasher, idGenerator),
      authenticateUser: new AuthenticateUserUseCase(userRepository, passwordHasher, sessionIssuer),
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
      tokenService,
    },
    passwordResetNotifier,
  };
}

function buildTestDependencies(): ServerDependencies {
  return buildTestContext().dependencies;
}

async function registerAndLogin(app: Express, email = "user@example.com", password = "StrongPass1") {
  await request(app).post("/auth/register").send({ email, password });
  const loginResponse = await request(app).post("/auth/login").send({ email, password });
  return loginResponse.body.token as string;
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
    const token = await registerAndLogin(app);

    const meResponse = await request(app).get("/users/me").set("Authorization", `Bearer ${token}`);

    expect(meResponse.status).toBe(200);
    expect(meResponse.body.id).toBeDefined();
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
    const response = await request(app).get("/users/me");

    expect(response.status).toBe(401);
  });

  it("rejects access to a protected route with a malformed token", async () => {
    const response = await request(app).get("/users/me").set("Authorization", "Bearer not-a-real-token");

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
        .get("/users/me")
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

describe("User Profile & Consent HTTP API (RF-006, RF-046)", () => {
  let app: Express;

  beforeEach(() => {
    app = createServer(buildTestDependencies());
  });

  describe("GET /users/me", () => {
    it("returns the authenticated user's profile with no name by default", async () => {
      const token = await registerAndLogin(app);

      const response = await request(app).get("/users/me").set("Authorization", `Bearer ${token}`);

      expect(response.status).toBe(200);
      expect(response.body).toEqual({ id: response.body.id, name: null, email: "user@example.com" });
    });
  });

  describe("PUT /users/me", () => {
    it("updates the authenticated user's name and email", async () => {
      const token = await registerAndLogin(app);

      const response = await request(app)
        .put("/users/me")
        .set("Authorization", `Bearer ${token}`)
        .send({ name: "Ana Souza", email: "ana@example.com" });

      expect(response.status).toBe(200);
      expect(response.body).toEqual({ id: response.body.id, name: "Ana Souza", email: "ana@example.com" });

      const profileResponse = await request(app).get("/users/me").set("Authorization", `Bearer ${token}`);
      expect(profileResponse.body.email).toBe("ana@example.com");
    });

    it("rejects updating to an email already used by another user", async () => {
      await registerAndLogin(app, "other@example.com");
      const token = await registerAndLogin(app, "user@example.com");

      const response = await request(app)
        .put("/users/me")
        .set("Authorization", `Bearer ${token}`)
        .send({ name: "Ana Souza", email: "other@example.com" });

      expect(response.status).toBe(400);
    });

    it("rejects a request body without a name field instead of returning a 500", async () => {
      const token = await registerAndLogin(app);

      const response = await request(app)
        .put("/users/me")
        .set("Authorization", `Bearer ${token}`)
        .send({ email: "ana@example.com" });

      expect(response.status).toBe(400);
    });

    it("rejects requests without a valid token", async () => {
      const response = await request(app).put("/users/me").send({ name: "Ana Souza", email: "ana@example.com" });

      expect(response.status).toBe(401);
    });
  });

  describe("POST /users/me/consent and GET /users/me/consent", () => {
    it("records a consent and lists it back in the history (RF-046)", async () => {
      const token = await registerAndLogin(app);

      const recordResponse = await request(app)
        .post("/users/me/consent")
        .set("Authorization", `Bearer ${token}`)
        .send({ version: "privacy-policy-v1" });

      expect(recordResponse.status).toBe(201);
      expect(recordResponse.body.version).toBe("privacy-policy-v1");

      const historyResponse = await request(app).get("/users/me/consent").set("Authorization", `Bearer ${token}`);

      expect(historyResponse.status).toBe(200);
      expect(historyResponse.body).toHaveLength(1);
      expect(historyResponse.body[0].version).toBe("privacy-policy-v1");
    });

    it("preserves consent history across multiple versions accepted over time", async () => {
      const token = await registerAndLogin(app);

      await request(app).post("/users/me/consent").set("Authorization", `Bearer ${token}`).send({ version: "v1" });
      await request(app).post("/users/me/consent").set("Authorization", `Bearer ${token}`).send({ version: "v2" });

      const historyResponse = await request(app).get("/users/me/consent").set("Authorization", `Bearer ${token}`);

      expect(historyResponse.body).toHaveLength(2);
    });

    it("rejects a request body without a version field instead of returning a 500", async () => {
      const token = await registerAndLogin(app);

      const response = await request(app)
        .post("/users/me/consent")
        .set("Authorization", `Bearer ${token}`)
        .send({});

      expect(response.status).toBe(400);
    });

    it("rejects requests without a valid token", async () => {
      const response = await request(app).get("/users/me/consent");

      expect(response.status).toBe(401);
    });
  });
});

describe("Password Reset HTTP API (RF-005)", () => {
  it("completes the full reset flow: request, confirm, then login with the new password", async () => {
    const { dependencies, passwordResetNotifier } = buildTestContext();
    const app = createServer(dependencies);

    await request(app).post("/auth/register").send({ email: "user@example.com", password: "StrongPass1" });

    const requestResponse = await request(app)
      .post("/auth/password-reset/request")
      .send({ email: "user@example.com" });
    expect(requestResponse.status).toBe(202);
    expect(passwordResetNotifier.sent).toHaveLength(1);

    const rawToken = passwordResetNotifier.sent[0].rawToken;
    const confirmResponse = await request(app)
      .post("/auth/password-reset/confirm")
      .send({ token: rawToken, newPassword: "NewStrongPass1" });
    expect(confirmResponse.status).toBe(200);

    const oldPasswordLogin = await request(app)
      .post("/auth/login")
      .send({ email: "user@example.com", password: "StrongPass1" });
    expect(oldPasswordLogin.status).toBe(401);

    const newPasswordLogin = await request(app)
      .post("/auth/login")
      .send({ email: "user@example.com", password: "NewStrongPass1" });
    expect(newPasswordLogin.status).toBe(200);
  });

  it("responds identically whether or not the email is registered (anti-enumeration)", async () => {
    const { dependencies, passwordResetNotifier } = buildTestContext();
    const app = createServer(dependencies);
    await request(app).post("/auth/register").send({ email: "user@example.com", password: "StrongPass1" });

    const knownEmailResponse = await request(app)
      .post("/auth/password-reset/request")
      .send({ email: "user@example.com" });
    const unknownEmailResponse = await request(app)
      .post("/auth/password-reset/request")
      .send({ email: "ghost@example.com" });

    expect(knownEmailResponse.status).toBe(unknownEmailResponse.status);
    expect(knownEmailResponse.body).toEqual(unknownEmailResponse.body);
    // Only the registered email actually triggers a notification.
    expect(passwordResetNotifier.sent).toHaveLength(1);
  });

  it("rejects a malformed email with a 400 (format validation is not an enumeration leak)", async () => {
    const { dependencies } = buildTestContext();
    const app = createServer(dependencies);

    const response = await request(app).post("/auth/password-reset/request").send({ email: "not-an-email" });

    expect(response.status).toBe(400);
  });

  it("invalidates every active refresh token of the user upon a successful reset", async () => {
    const { dependencies, passwordResetNotifier } = buildTestContext();
    const app = createServer(dependencies);
    await request(app).post("/auth/register").send({ email: "user@example.com", password: "StrongPass1" });
    const loginResponse = await request(app)
      .post("/auth/login")
      .send({ email: "user@example.com", password: "StrongPass1" });

    await request(app).post("/auth/password-reset/request").send({ email: "user@example.com" });
    await request(app)
      .post("/auth/password-reset/confirm")
      .send({ token: passwordResetNotifier.sent[0].rawToken, newPassword: "NewStrongPass1" });

    const refreshResponse = await request(app)
      .post("/auth/refresh")
      .send({ refreshToken: loginResponse.body.refreshToken });
    expect(refreshResponse.status).toBe(401);
  });

  it("rejects reuse of an already-confirmed reset token", async () => {
    const { dependencies, passwordResetNotifier } = buildTestContext();
    const app = createServer(dependencies);
    await request(app).post("/auth/register").send({ email: "user@example.com", password: "StrongPass1" });
    await request(app).post("/auth/password-reset/request").send({ email: "user@example.com" });
    const rawToken = passwordResetNotifier.sent[0].rawToken;

    await request(app).post("/auth/password-reset/confirm").send({ token: rawToken, newPassword: "NewStrongPass1" });
    const secondAttempt = await request(app)
      .post("/auth/password-reset/confirm")
      .send({ token: rawToken, newPassword: "AnotherStrongPass1" });

    expect(secondAttempt.status).toBe(401);
  });

  it("rejects an unknown reset token", async () => {
    const { dependencies } = buildTestContext();
    const app = createServer(dependencies);

    const response = await request(app)
      .post("/auth/password-reset/confirm")
      .send({ token: "never-issued", newPassword: "NewStrongPass1" });

    expect(response.status).toBe(401);
  });

  it("rejects a weak new password", async () => {
    const { dependencies, passwordResetNotifier } = buildTestContext();
    const app = createServer(dependencies);
    await request(app).post("/auth/register").send({ email: "user@example.com", password: "StrongPass1" });
    await request(app).post("/auth/password-reset/request").send({ email: "user@example.com" });

    const response = await request(app)
      .post("/auth/password-reset/confirm")
      .send({ token: passwordResetNotifier.sent[0].rawToken, newPassword: "short" });

    expect(response.status).toBe(400);
  });

  it("rejects a request body without an email field instead of returning a 500", async () => {
    const { dependencies } = buildTestContext();
    const app = createServer(dependencies);

    const response = await request(app).post("/auth/password-reset/request").send({});

    expect(response.status).toBe(400);
  });

  it("rejects a confirmation body without token/newPassword instead of returning a 500", async () => {
    const { dependencies } = buildTestContext();
    const app = createServer(dependencies);

    const response = await request(app).post("/auth/password-reset/confirm").send({});

    expect(response.status).toBe(400);
  });
});
