import { beforeEach, describe, expect, it } from "vitest";
import request from "supertest";
import type { Express } from "express";
import { createServer, type ServerDependencies } from "./server";
import { RegisterUserUseCase } from "./application/use-cases/RegisterUserUseCase";
import { AuthenticateUserUseCase } from "./application/use-cases/AuthenticateUserUseCase";
import { InMemoryUserRepository } from "./adapters/out/persistence/InMemoryUserRepository";
import { BcryptPasswordHasher } from "./adapters/out/security/BcryptPasswordHasher";
import { JwtTokenService } from "./adapters/out/security/JwtTokenService";
import { CryptoIdGenerator } from "./adapters/out/security/CryptoIdGenerator";

function buildTestDependencies(): ServerDependencies {
  const userRepository = new InMemoryUserRepository();
  const passwordHasher = new BcryptPasswordHasher();
  const tokenService = new JwtTokenService("test-secret");
  const idGenerator = new CryptoIdGenerator();

  return {
    registerUser: new RegisterUserUseCase(userRepository, passwordHasher, idGenerator),
    authenticateUser: new AuthenticateUserUseCase(userRepository, passwordHasher, tokenService),
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

  it("logs in and accesses a protected route with the issued token", async () => {
    await request(app).post("/auth/register").send({ email: "user@example.com", password: "StrongPass1" });

    const loginResponse = await request(app).post("/auth/login").send({
      email: "user@example.com",
      password: "StrongPass1",
    });

    expect(loginResponse.status).toBe(200);
    const token = loginResponse.body.token;
    expect(typeof token).toBe("string");

    const meResponse = await request(app).get("/auth/me").set("Authorization", `Bearer ${token}`);

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
});
