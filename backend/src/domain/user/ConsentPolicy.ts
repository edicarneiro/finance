import { InvalidConsentVersionError } from "./errors/InvalidConsentVersionError";

export function assertValidConsentVersion(version: string): void {
  if (version.trim().length < 1) {
    throw new InvalidConsentVersionError();
  }
}
