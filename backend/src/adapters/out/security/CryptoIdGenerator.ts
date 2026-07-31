import { randomUUID } from "node:crypto";
import { IdGenerator } from "../../../application/ports/IdGenerator";

export class CryptoIdGenerator implements IdGenerator {
  generate(): string {
    return randomUUID();
  }
}
