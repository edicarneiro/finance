import { MIN_PASSWORD_LENGTH, WeakPasswordError } from "./errors/WeakPasswordError";

export function assertStrongPassword(plainPassword: string): void {
  if (plainPassword.length < MIN_PASSWORD_LENGTH) {
    throw new WeakPasswordError();
  }
}
