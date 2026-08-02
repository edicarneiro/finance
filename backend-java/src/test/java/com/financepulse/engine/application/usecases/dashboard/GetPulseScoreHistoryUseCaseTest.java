package com.financepulse.engine.application.usecases.dashboard;

import static org.assertj.core.api.Assertions.assertThat;

import com.financepulse.engine.domain.pulsescore.PulseScoreSnapshot;
import com.financepulse.engine.testsupport.FixedClock;
import com.financepulse.engine.testsupport.InMemoryPulseScoreRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class GetPulseScoreHistoryUseCaseTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 7, 31);

    private final InMemoryPulseScoreRepository pulseScoreRepository = new InMemoryPulseScoreRepository();
    private final GetPulseScoreHistoryUseCase useCase = new GetPulseScoreHistoryUseCase(pulseScoreRepository, new FixedClock(TODAY));

    @Test
    void returnsSnapshotsWithinTheWindowMostRecentFirst() {
        saveSnapshot("snap-1", LocalDate.of(2026, 7, 1), "60");
        saveSnapshot("snap-2", LocalDate.of(2026, 7, 20), "70");
        saveSnapshot("snap-3", TODAY, "80");

        GetPulseScoreHistoryUseCase.Output output = useCase.execute(new GetPulseScoreHistoryUseCase.Input("user-1", 90));

        assertThat(output.history()).extracting("date").containsExactly(TODAY, LocalDate.of(2026, 7, 20), LocalDate.of(2026, 7, 1));
        assertThat(output.history().get(0).score()).isEqualByComparingTo("80");
    }

    @Test
    void excludesSnapshotsOlderThanTheRequestedWindow() {
        saveSnapshot("snap-old", LocalDate.of(2026, 1, 1), "50");
        saveSnapshot("snap-recent", TODAY, "90");

        GetPulseScoreHistoryUseCase.Output output = useCase.execute(new GetPulseScoreHistoryUseCase.Input("user-1", 30));

        assertThat(output.history()).hasSize(1);
        assertThat(output.history().get(0).date()).isEqualTo(TODAY);
    }

    @Test
    void doesNotMixSnapshotsFromAnotherUser() {
        pulseScoreRepository.saveOrUpdate(PulseScoreSnapshot.create(
                "snap-other", "another-user", TODAY, new BigDecimal("99"), null, null, null, new BigDecimal("50"), "pulse-v0-provisional"));
        saveSnapshot("snap-mine", TODAY, "40");

        GetPulseScoreHistoryUseCase.Output output = useCase.execute(new GetPulseScoreHistoryUseCase.Input("user-1", 90));

        assertThat(output.history()).hasSize(1);
        assertThat(output.history().get(0).score()).isEqualByComparingTo("40");
    }

    private void saveSnapshot(String id, LocalDate date, String overallScore) {
        pulseScoreRepository.saveOrUpdate(PulseScoreSnapshot.create(
                id, "user-1", date, new BigDecimal(overallScore), null, null, null, new BigDecimal("50"), "pulse-v0-provisional"));
    }
}
