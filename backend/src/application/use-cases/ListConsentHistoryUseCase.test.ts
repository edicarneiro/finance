import { beforeEach, describe, expect, it } from "vitest";
import { ListConsentHistoryUseCase } from "./ListConsentHistoryUseCase";
import { InMemoryConsentRepository } from "../../adapters/out/persistence/InMemoryConsentRepository";
import { ConsentRecord } from "../../domain/user/ConsentRecord";

describe("ListConsentHistoryUseCase", () => {
  let consentRepository: InMemoryConsentRepository;
  let useCase: ListConsentHistoryUseCase;

  beforeEach(() => {
    consentRepository = new InMemoryConsentRepository();
    useCase = new ListConsentHistoryUseCase(consentRepository);
  });

  it("returns an empty list for a user with no consent history", async () => {
    expect(await useCase.execute({ userId: "user-1" })).toEqual([]);
  });

  it("returns every consent record for a user (RF-046 audit trail)", async () => {
    await consentRepository.save(
      ConsentRecord.give({
        id: "consent-1",
        userId: "user-1",
        version: "privacy-policy-v1",
        acceptedAt: new Date("2026-01-01T00:00:00.000Z"),
      }),
    );

    const history = await useCase.execute({ userId: "user-1" });

    expect(history).toEqual([
      { id: "consent-1", version: "privacy-policy-v1", acceptedAt: new Date("2026-01-01T00:00:00.000Z") },
    ]);
  });
});
