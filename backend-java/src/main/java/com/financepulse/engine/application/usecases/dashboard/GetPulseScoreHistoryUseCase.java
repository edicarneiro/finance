package com.financepulse.engine.application.usecases.dashboard;

import com.financepulse.engine.application.ports.Clock;
import com.financepulse.engine.application.ports.PulseScoreRepository;
import com.financepulse.engine.domain.pulsescore.PulseScoreSnapshot;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/** RF-035: evolução histórica do Pulse Score, a partir dos snapshots já persistidos por GetDashboardUseCase (sem recálculo, ver ADR-0020). */
public class GetPulseScoreHistoryUseCase {

    static final int DEFAULT_WINDOW_DAYS = 90;
    static final int MAX_WINDOW_DAYS = 365;

    private final PulseScoreRepository pulseScoreRepository;
    private final Clock clock;

    public GetPulseScoreHistoryUseCase(PulseScoreRepository pulseScoreRepository, Clock clock) {
        this.pulseScoreRepository = pulseScoreRepository;
        this.clock = clock;
    }

    public Output execute(Input input) {
        int windowDays = Math.min(Math.max(input.windowDays(), 1), MAX_WINDOW_DAYS);
        LocalDate since = clock.today().minusDays(windowDays);

        List<HistoryEntry> history = pulseScoreRepository.findAllByUserId(input.userId()).stream()
                .filter(snapshot -> !snapshot.getScoreDate().isBefore(since))
                .sorted(Comparator.comparing(PulseScoreSnapshot::getScoreDate).reversed())
                .map(snapshot -> new HistoryEntry(snapshot.getScoreDate(), snapshot.getOverallScore(), snapshot.getFormulaVersion()))
                .toList();

        return new Output(history);
    }

    public record Input(String userId, int windowDays) {

        public Input(String userId) {
            this(userId, DEFAULT_WINDOW_DAYS);
        }
    }

    public record Output(List<HistoryEntry> history) {
    }

    public record HistoryEntry(LocalDate date, BigDecimal score, String formulaVersion) {
    }
}
