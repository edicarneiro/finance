export const MIN_PASSWORD_LENGTH = 8;

export class WeakPasswordError extends Error {
  constructor() {
    super(`A senha deve ter pelo menos ${MIN_PASSWORD_LENGTH} caracteres.`);
    this.name = "WeakPasswordError";
  }
}
