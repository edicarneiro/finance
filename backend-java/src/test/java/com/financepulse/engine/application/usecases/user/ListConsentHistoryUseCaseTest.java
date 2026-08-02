package com.financepulse.engine.application.usecases.user;

import static org.assertj.core.api.Assertions.assertThat;

import com.financepulse.engine.testsupport.InMemoryConsentRepository;
import com.financepulse.engine.testsupport.SequentialIdGenerator;
import org.junit.jupiter.api.Test;

class ListConsentHistoryUseCaseTest {

    private final InMemoryConsentRepository repository = new InMemoryConsentRepository();
    private final RecordConsentUseCase recordConsentUseCase = new RecordConsentUseCase(repository, new SequentialIdGenerator("consent"));
    private final ListConsentHistoryUseCase useCase = new ListConsentHistoryUseCase(repository);

    @Test
    void listsThePreviouslyRecordedConsentsMostRecentFirst() throws InterruptedException {
        recordConsentUseCase.execute(new RecordConsentUseCase.Input("user-1", "2026-08-01"));
        Thread.sleep(5);
        recordConsentUseCase.execute(new RecordConsentUseCase.Input("user-1", "2026-09-01"));

        ListConsentHistoryUseCase.Output output = useCase.execute(new ListConsentHistoryUseCase.Input("user-1"));

        assertThat(output.consents()).extracting("version").containsExactly("2026-09-01", "2026-08-01");
    }

    @Test
    void doesNotMixConsentsFromAnotherUser() {
        recordConsentUseCase.execute(new RecordConsentUseCase.Input("another-user", "2026-08-01"));

        ListConsentHistoryUseCase.Output output = useCase.execute(new ListConsentHistoryUseCase.Input("user-1"));

        assertThat(output.consents()).isEmpty();
    }

    @Test
    void returnsAnEmptyListWhenTheUserHasNeverConsented() {
        ListConsentHistoryUseCase.Output output = useCase.execute(new ListConsentHistoryUseCase.Input("user-1"));

        assertThat(output.consents()).isEmpty();
    }
}
