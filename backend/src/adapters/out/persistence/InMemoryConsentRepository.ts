import { ConsentRecord } from "../../../domain/user/ConsentRecord";
import { ConsentRepository } from "../../../application/ports/ConsentRepository";

export class InMemoryConsentRepository implements ConsentRepository {
  private readonly records: ConsentRecord[] = [];

  async save(record: ConsentRecord): Promise<void> {
    this.records.push(record);
  }

  async findAllForUser(userId: string): Promise<ConsentRecord[]> {
    return this.records.filter((record) => record.userId === userId);
  }
}
