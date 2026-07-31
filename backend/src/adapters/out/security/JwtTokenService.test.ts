import { describe, expect, it } from "vitest";
import { JwtTokenService } from "./JwtTokenService";

describe("JwtTokenService", () => {
  it("issues a token that can be verified back to the same user id (RF-008)", () => {
    const service = new JwtTokenService("test-secret");

    const token = service.issue("user-1");

    expect(service.verify(token)).toEqual({ userId: "user-1" });
  });

  it("rejects a token signed with a different secret", () => {
    const service = new JwtTokenService("test-secret");
    const otherService = new JwtTokenService("another-secret");

    const token = otherService.issue("user-1");

    expect(service.verify(token)).toBeNull();
  });

  it("rejects a malformed token", () => {
    const service = new JwtTokenService("test-secret");

    expect(service.verify("not-a-token")).toBeNull();
  });
});
