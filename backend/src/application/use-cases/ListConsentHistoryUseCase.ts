import { ConsentRepository } from "../ports/ConsentRepository";
import { ConsentRecordView } from "./RecordConsentUseCase";

export interface ListConsentHistoryInput {
  userId: string;
}

export class ListConsentHistoryUseCase {
  constructor(private readonly consentRepository: ConsentRepository) {}

  async execute(input: ListConsentHistoryInput): Promise<ConsentRecordView[]> {
    const records = await this.consentRepository.findAllForUser(input.userId);
    return records.map((record) => ({ id: record.id, version: record.version, acceptedAt: record.acceptedAt }));
  }
}
