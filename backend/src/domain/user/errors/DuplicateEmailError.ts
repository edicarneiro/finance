export class DuplicateEmailError extends Error {
  constructor(email: string) {
    super(`Já existe uma conta cadastrada com o e-mail "${email}".`);
    this.name = "DuplicateEmailError";
  }
}
