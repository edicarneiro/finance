package com.financepulse.engine.application.usecases.goal;

import com.financepulse.engine.application.ports.Clock;
import com.financepulse.engine.application.ports.GoalRepository;
import com.financepulse.engine.domain.goal.Goal;
import com.financepulse.engine.domain.goal.errors.GoalNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** RF-030: nome, valor-alvo, prazo e limiares são editáveis — a associação de conta/categoria é imutável (ver ADR-0019). */
public class UpdateGoalUseCase {

    private final GoalRepository goalRepository;
    private final Clock clock;

    public UpdateGoalUseCase(GoalRepository goalRepository, Clock clock) {
        this.goalRepository = goalRepository;
        this.clock = clock;
    }

    public void execute(Input input) {
        Goal goal = goalRepository.findByIdAndUserId(input.goalId(), input.userId()).orElseThrow(GoalNotFoundException::new);

        goalRepository.update(
                goal.withDetails(input.name(), input.targetAmount(), input.deadline(), input.progressAlertThresholds(), clock.today()));
    }

    public record Input(String userId, String goalId, String name, BigDecimal targetAmount, LocalDate deadline, List<Integer> progressAlertThresholds) {
    }
}
