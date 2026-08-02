package com.financepulse.engine.adapters.in.web.dto;

import com.financepulse.engine.application.usecases.budget.GetBudgetHistoryUseCase.PeriodPerformance;
import java.math.BigDecimal;
import java.time.LocalDate;

public record BudgetPeriodPerformanceResponse(LocalDate periodStart, LocalDate periodEnd, BigDecimal consumedAmount, BigDecimal consumedPercentage) {

    public static BudgetPeriodPerformanceResponse from(PeriodPerformance performance) {
        return new BudgetPeriodPerformanceResponse(
                performance.periodStart(), performance.periodEnd(), performance.consumedAmount(), performance.consumedPercentage());
    }
}
