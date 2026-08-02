package com.financepulse.engine.application.usecases.goal;

import com.financepulse.engine.application.ports.GoalRepository;
import com.financepulse.engine.domain.goal.errors.GoalNotFoundException;

/** Exclusão definitiva — nada referencia uma meta, então não há bloqueio de exclusão. */
public class DeleteGoalUseCase {

    private final GoalRepository goalRepository;

    public DeleteGoalUseCase(GoalRepository goalRepository) {
        this.goalRepository = goalRepository;
    }

    public void execute(Input input) {
        goalRepository.findByIdAndUserId(input.goalId(), input.userId()).orElseThrow(GoalNotFoundException::new);

        goalRepository.deleteByIdAndUserId(input.goalId(), input.userId());
    }

    public record Input(String userId, String goalId) {
    }
}
