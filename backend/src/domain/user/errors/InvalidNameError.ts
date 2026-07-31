export const MAX_NAME_LENGTH = 100;

export class InvalidNameError extends Error {
  constructor() {
    super(`O nome deve ter entre 1 e ${MAX_NAME_LENGTH} caracteres.`);
    this.name = "InvalidNameError";
  }
}
