export class MfaNotEnrolledError extends Error {
  constructor() {
    super("Nenhum cadastro de MFA pendente de confirmação ou ativo para esta conta.");
    this.name = "MfaNotEnrolledError";
  }
}
