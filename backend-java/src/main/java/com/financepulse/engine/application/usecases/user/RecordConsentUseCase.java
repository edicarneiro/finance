package com.financepulse.engine.application.usecases.user;

import com.financepulse.engine.application.ports.ConsentRepository;
import com.financepulse.engine.application.ports.IdGenerator;
import com.financepulse.engine.domain.user.ConsentRecord;
import java.time.Instant;

/** RF-046 (ver ADR-0023): registra um novo aceite — trilha append-only, nunca atualizada ou apagada. */
public class RecordConsentUseCase {

    private final ConsentRepository consentRepository;
    private final IdGenerator idGenerator;

    public RecordConsentUseCase(ConsentRepository consentRepository, IdGenerator idGenerator) {
        this.consentRepository = consentRepository;
        this.idGenerator = idGenerator;
    }

    public Output execute(Input input) {
        ConsentRecord consentRecord = ConsentRecord.create(idGenerator.generate(), input.userId(), input.version());

        consentRepository.save(consentRecord);

        return new Output(consentRecord.getId(), consentRecord.getAcceptedAt());
    }

    public record Input(String userId, String version) {
    }

    public record Output(String consentId, Instant acceptedAt) {
    }
}
