export class InvalidOrExpiredResetTokenError extends Error {
  constructor() {
    super("Token de recuperação de senha inválido, expirado ou já utilizado.");
    this.name = "InvalidOrExpiredResetTokenError";
  }
}
