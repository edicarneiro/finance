package com.financepulse.engine.application.usecases.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.financepulse.engine.domain.user.errors.InvalidConsentVersionException;
import com.financepulse.engine.testsupport.InMemoryConsentRepository;
import com.financepulse.engine.testsupport.SequentialIdGenerator;
import org.junit.jupiter.api.Test;

class RecordConsentUseCaseTest {

    private final InMemoryConsentRepository repository = new InMemoryConsentRepository();
    private final RecordConsentUseCase useCase = new RecordConsentUseCase(repository, new SequentialIdGenerator("consent"));

    @Test
    void recordsANewConsentAcceptance() {
        RecordConsentUseCase.Output output = useCase.execute(new RecordConsentUseCase.Input("user-1", "2026-08-01"));

        assertThat(output.consentId()).isEqualTo("consent-1");
        assertThat(repository.findAllByUserId("user-1")).hasSize(1);
        assertThat(repository.findAllByUserId("user-1").get(0).getVersion()).isEqualTo("2026-08-01");
    }

    @Test
    void recordingTwoAcceptancesKeepsBothAsAnAppendOnlyHistory() {
        useCase.execute(new RecordConsentUseCase.Input("user-1", "2026-08-01"));
        useCase.execute(new RecordConsentUseCase.Input("user-1", "2026-09-01"));

        assertThat(repository.findAllByUserId("user-1")).hasSize(2);
    }

    @Test
    void rejectsABlankVersion() {
        assertThatThrownBy(() -> useCase.execute(new RecordConsentUseCase.Input("user-1", "  ")))
                .isInstanceOf(InvalidConsentVersionException.class);
    }
}
