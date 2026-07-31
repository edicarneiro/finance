import { ConsentRecord } from "../../domain/user/ConsentRecord";

export interface ConsentRepository {
  save(record: ConsentRecord): Promise<void>;
  findAllForUser(userId: string): Promise<ConsentRecord[]>;
}
