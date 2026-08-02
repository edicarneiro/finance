package com.financepulse.engine.adapters.in.web.dto;

import com.financepulse.engine.application.usecases.budget.ListBudgetsUseCase.BudgetView;
import com.financepulse.engine.domain.budget.BudgetPeriodType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record BudgetResponse(
        String id,
        String categoryId,
        BigDecimal limitAmount,
        BudgetPeriodType periodType,
        List<Integer> alertThresholds,
        LocalDate periodStart,
        LocalDate periodEnd,
        BigDecimal consumedAmount,
        BigDecimal consumedPercentage,
        List<Integer> thresholdsCrossed) {

    public static BudgetResponse from(BudgetView view) {
        return new BudgetResponse(
                view.id(),
                view.categoryId(),
                view.limitAmount(),
                view.periodType(),
                view.alertThresholds(),
                view.periodStart(),
                view.periodEnd(),
                view.consumedAmount(),
                view.consumedPercentage(),
                view.thresholdsCrossed());
    }
}
