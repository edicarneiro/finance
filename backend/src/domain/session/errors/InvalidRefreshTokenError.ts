export class InvalidRefreshTokenError extends Error {
  constructor() {
    super("Refresh token inválido, expirado ou revogado.");
    this.name = "InvalidRefreshTokenError";
  }
}
