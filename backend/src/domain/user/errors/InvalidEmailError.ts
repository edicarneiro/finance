export class InvalidEmailError extends Error {
  constructor(rawValue: string) {
    super(`"${rawValue}" não é um endereço de e-mail válido.`);
    this.name = "InvalidEmailError";
  }
}
