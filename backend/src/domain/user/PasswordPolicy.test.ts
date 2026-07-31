import { describe, expect, it } from "vitest";
import { assertStrongPassword } from "./PasswordPolicy";
import { WeakPasswordError } from "./errors/WeakPasswordError";

describe("assertStrongPassword", () => {
  it("accepts a password with at least 8 characters", () => {
    expect(() => assertStrongPassword("StrongPass1")).not.toThrow();
  });

  it("rejects a password shorter than 8 characters", () => {
    expect(() => assertStrongPassword("Sh0rt")).toThrow(WeakPasswordError);
  });
});
