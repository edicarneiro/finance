package com.financepulse.engine.adapters.in.web.dto;

import com.financepulse.engine.application.usecases.report.GetPeriodComparisonReportUseCase.CategoryComparison;
import com.financepulse.engine.application.usecases.report.GetPeriodComparisonReportUseCase.Output;
import com.financepulse.engine.application.usecases.report.GetPeriodComparisonReportUseCase.PeriodSummary;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PeriodComparisonReportResponse(
        PeriodSummaryResponse periodA, PeriodSummaryResponse periodB, List<CategoryComparisonResponse> categoryComparisons) {

    public static PeriodComparisonReportResponse from(Output output) {
        return new PeriodComparisonReportResponse(
                PeriodSummaryResponse.from(output.periodA()),
                PeriodSummaryResponse.from(output.periodB()),
                output.categoryComparisons().stream().map(CategoryComparisonResponse::from).toList());
    }

    public record PeriodSummaryResponse(LocalDate startDate, LocalDate endDate, BigDecimal totalIncome, BigDecimal totalExpense, BigDecimal net) {

        public static PeriodSummaryResponse from(PeriodSummary summary) {
            return new PeriodSummaryResponse(summary.startDate(), summary.endDate(), summary.totalIncome(), summary.totalExpense(), summary.net());
        }
    }

    /** {@code percentageChange} é {@code null} quando o valor no período A é zero (base zero, variação indefinida). */
    public record CategoryComparisonResponse(
            String categoryId, String categoryName, BigDecimal amountPeriodA, BigDecimal amountPeriodB, BigDecimal delta, BigDecimal percentageChange) {

        public static CategoryComparisonResponse from(CategoryComparison comparison) {
            return new CategoryComparisonResponse(
                    comparison.categoryId(), comparison.categoryName(), comparison.amountPeriodA(), comparison.amountPeriodB(), comparison.delta(),
                    comparison.percentageChange());
        }
    }
}
