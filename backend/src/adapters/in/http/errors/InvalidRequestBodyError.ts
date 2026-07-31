export class InvalidRequestBodyError extends Error {
  constructor(message: string) {
    super(message);
    this.name = "InvalidRequestBodyError";
  }
}
