import { ConsentRecord } from "../../domain/user/ConsentRecord";
import { assertValidConsentVersion } from "../../domain/user/ConsentPolicy";
import { UserNotFoundError } from "../../domain/user/errors/UserNotFoundError";
import { UserRepository } from "../ports/UserRepository";
import { ConsentRepository } from "../ports/ConsentRepository";
import { IdGenerator } from "../ports/IdGenerator";
import { Clock } from "../ports/Clock";

export interface RecordConsentInput {
  userId: string;
  version: string;
}

export interface ConsentRecordView {
  id: string;
  version: string;
  acceptedAt: Date;
}

export class RecordConsentUseCase {
  constructor(
    private readonly userRepository: UserRepository,
    private readonly consentRepository: ConsentRepository,
    private readonly idGenerator: IdGenerator,
    private readonly clock: Clock,
  ) {}

  async execute(input: RecordConsentInput): Promise<ConsentRecordView> {
    const user = await this.userRepository.findById(input.userId);
    if (!user) {
      throw new UserNotFoundError();
    }

    assertValidConsentVersion(input.version);

    const record = ConsentRecord.give({
      id: this.idGenerator.generate(),
      userId: input.userId,
      version: input.version,
      acceptedAt: this.clock.now(),
    });

    await this.consentRepository.save(record);

    return { id: record.id, version: record.version, acceptedAt: record.acceptedAt };
  }
}
