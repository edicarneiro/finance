export class InvalidConsentVersionError extends Error {
  constructor() {
    super('O campo "version" do consentimento é obrigatório e não pode ser vazio.');
    this.name = "InvalidConsentVersionError";
  }
}
