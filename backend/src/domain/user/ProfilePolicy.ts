import { InvalidNameError, MAX_NAME_LENGTH } from "./errors/InvalidNameError";

export function assertValidName(name: string): void {
  const trimmed = name.trim();

  if (trimmed.length < 1 || trimmed.length > MAX_NAME_LENGTH) {
    throw new InvalidNameError();
  }
}
