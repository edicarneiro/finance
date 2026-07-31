import { describe, expect, it, vi } from "vitest";
import request from "supertest";
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
    const container = buildContainer({ databasePath: ":memory:", jwtSecret: "integration-test-secret" });
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
  });
});
