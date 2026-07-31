export class InvalidOrExpiredMfaChallengeError extends Error {
  constructor() {
    super("Desafio de autenticação multifator inválido, expirado ou já utilizado.");
    this.name = "InvalidOrExpiredMfaChallengeError";
  }
}
