package com.financepulse.engine.adapters.in.web.dto;

import com.financepulse.engine.application.usecases.goal.ListGoalsUseCase.GoalView;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record GoalResponse(
        String id,
        String name,
        BigDecimal targetAmount,
        LocalDate deadline,
        String accountId,
        String categoryId,
        List<Integer> progressAlertThresholds,
        BigDecimal currentAmount,
        BigDecimal progressPercentage,
        List<Integer> thresholdsCrossed,
        boolean achieved,
        boolean overdue) {

    public static GoalResponse from(GoalView view) {
        return new GoalResponse(
                view.id(),
                view.name(),
                view.targetAmount(),
                view.deadline(),
                view.accountId(),
                view.categoryId(),
                view.progressAlertThresholds(),
                view.currentAmount(),
                view.progressPercentage(),
                view.thresholdsCrossed(),
                view.achieved(),
                view.overdue());
    }
}
