import type { IdGenerator } from "../application/ports/IdGenerator";

export class SequentialIdGenerator implements IdGenerator {
  private counter = 0;

  constructor(private readonly prefix: string = "id") {}

  generate(): string {
    this.counter += 1;
    return `${this.prefix}-${this.counter}`;
  }
}
