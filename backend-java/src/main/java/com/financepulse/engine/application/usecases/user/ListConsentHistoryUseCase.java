package com.financepulse.engine.application.usecases.user;

import com.financepulse.engine.application.ports.ConsentRepository;
import com.financepulse.engine.domain.user.ConsentRecord;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

/** RF-046 (ver ADR-0023): histórico completo de consentimentos do usuário, mais recente primeiro. */
public class ListConsentHistoryUseCase {

    private final ConsentRepository consentRepository;

    public ListConsentHistoryUseCase(ConsentRepository consentRepository) {
        this.consentRepository = consentRepository;
    }

    public Output execute(Input input) {
        List<ConsentView> views = consentRepository.findAllByUserId(input.userId()).stream()
                .sorted(Comparator.comparing(ConsentRecord::getAcceptedAt).reversed())
                .map(record -> new ConsentView(record.getId(), record.getVersion(), record.getAcceptedAt()))
                .toList();

        return new Output(views);
    }

    public record Input(String userId) {
    }

    public record Output(List<ConsentView> consents) {
    }

    public record ConsentView(String id, String version, Instant acceptedAt) {
    }
}
