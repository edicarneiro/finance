export class InvalidMfaCodeError extends Error {
  constructor() {
    super("Código de autenticação multifator inválido.");
    this.name = "InvalidMfaCodeError";
  }
}
