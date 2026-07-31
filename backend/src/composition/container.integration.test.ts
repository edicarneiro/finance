import { describe, expect, it, vi } from "vitest";
import request from "supertest";
import { randomBytes } from "node:crypto";
import { generate } from "otplib";
import { buildContainer } from "./container";
import { createServer } from "../server";

/**
 * Every other integration test wires in-memory adapters (fast, deterministic).
 * This file instead exercises the *real* composition root — SQLite, bcrypt,
 * JWT — end to end, so a bug isolated to a production adapter (e.g. an
 * UPDATE statement silently dropping a column) cannot hide behind
 * in-memory-only test coverage. Found the hard way during Phase 2.3 review:
 * SqliteUserRepository.update() did not persist password_hash changes, which
 * would have made the entire password-reset flow silently no-op in production.
 */
describe("Composition root smoke test (real SQLite/bcrypt/JWT adapters)", () => {
  it("supports the full register → login → edit profile → password reset → login-with-new-password flow", async () => {
    const container = buildContainer({
      databasePath: ":memory:",
      jwtSecret: "integration-test-secret",
      mfaEncryptionKey: randomBytes(32),
    });
    const app = createServer(container);

    const registerResponse = await request(app)
      .post("/auth/register")
      .send({ email: "user@example.com", password: "StrongPass1" });
    expect(registerResponse.status).toBe(201);

    const loginResponse = await request(app)
      .post("/auth/login")
      .send({ email: "user@example.com", password: "StrongPass1" });
    expect(loginResponse.status).toBe(200);

    const updateResponse = await request(app)
      .put("/users/me")
      .set("Authorization", `Bearer ${loginResponse.body.token}`)
      .send({ name: "Ana Souza", email: "user@example.com" });
    expect(updateResponse.status).toBe(200);

    // ConsolePasswordResetNotifier (the real adapter) only logs the reset token —
    // captured here to drive the confirmation step through the real HTTP + SQLite path.
    const logSpy = vi.spyOn(console, "log").mockImplementation(() => undefined);
    await request(app).post("/auth/password-reset/request").send({ email: "user@example.com" });
    const loggedMessage = logSpy.mock.calls[0].join(" ");
    logSpy.mockRestore();
    const rawToken = /token: (\S+) /.exec(loggedMessage)?.[1];
    expect(rawToken).toBeDefined();

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

    // The earlier profile edit must have survived the later password-only update() call.
    const profileResponse = await request(app)
      .get("/users/me")
      .set("Authorization", `Bearer ${newPasswordLogin.body.token}`);
    expect(profileResponse.body.name).toBe("Ana Souza");

    // Phase 2.4 (RF-007) added yet another column (deleted_at) to the same
    // pre-existing UPDATE statement — exercised here against the real adapter
    // for the same reason the rest of this file exists (rules.md §3).
    const deleteResponse = await request(app)
      .delete("/users/me")
      .set("Authorization", `Bearer ${newPasswordLogin.body.token}`)
      .send({ password: "NewStrongPass1" });
    expect(deleteResponse.status).toBe(204);

    const loginAfterDeletion = await request(app)
      .post("/auth/login")
      .send({ email: "user@example.com", password: "NewStrongPass1" });
    expect(loginAfterDeletion.status).toBe(401);
  });

  it("supports the full MFA enroll → confirm → login-with-challenge flow with a real TOTP code (RF-004, ADR-0012)", async () => {
    const container = buildContainer({
      databasePath: ":memory:",
      jwtSecret: "integration-test-secret",
      mfaEncryptionKey: randomBytes(32),
    });
    const app = createServer(container);

    await request(app).post("/auth/register").send({ email: "mfa-user@example.com", password: "StrongPass1" });
    const initialLogin = await request(app)
      .post("/auth/login")
      .send({ email: "mfa-user@example.com", password: "StrongPass1" });
    expect(initialLogin.body.mfaRequired).toBe(false);

    const enrollResponse = await request(app)
      .post("/users/me/mfa/enroll")
      .set("Authorization", `Bearer ${initialLogin.body.token}`)
      .send({ password: "StrongPass1" });
    expect(enrollResponse.status).toBe(201);

    // Real RFC 6238 code, generated the same way an authenticator app would.
    const validCode = await generate({ secret: enrollResponse.body.secret });
    const confirmResponse = await request(app)
      .post("/users/me/mfa/confirm")
      .set("Authorization", `Bearer ${initialLogin.body.token}`)
      .send({ code: validCode });
    expect(confirmResponse.status).toBe(200);

    const loginWithMfa = await request(app)
      .post("/auth/login")
      .send({ email: "mfa-user@example.com", password: "StrongPass1" });
    expect(loginWithMfa.status).toBe(200);
    expect(loginWithMfa.body.mfaRequired).toBe(true);
    expect(loginWithMfa.body.token).toBeUndefined();

    const secondValidCode = await generate({ secret: enrollResponse.body.secret });
    const mfaLoginResponse = await request(app)
      .post("/auth/login/mfa")
      .send({ challengeToken: loginWithMfa.body.challengeToken, code: secondValidCode });
    expect(mfaLoginResponse.status).toBe(200);
    expect(typeof mfaLoginResponse.body.token).toBe("string");

    const profileResponse = await request(app)
      .get("/users/me")
      .set("Authorization", `Bearer ${mfaLoginResponse.body.token}`);
    expect(profileResponse.status).toBe(200);
    expect(profileResponse.body.email).toBe("mfa-user@example.com");
  });
});
