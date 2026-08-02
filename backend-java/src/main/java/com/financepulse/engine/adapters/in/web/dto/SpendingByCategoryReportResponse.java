package com.financepulse.engine.adapters.in.web.dto;

import com.financepulse.engine.application.usecases.report.GetSpendingByCategoryReportUseCase.CategoryAmount;
import com.financepulse.engine.application.usecases.report.GetSpendingByCategoryReportUseCase.Output;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record SpendingByCategoryReportResponse(
        LocalDate startDate, LocalDate endDate, BigDecimal totalExpense, List<CategoryAmountResponse> categories) {

    public static SpendingByCategoryReportResponse from(Output output) {
        return new SpendingByCategoryReportResponse(
                output.startDate(), output.endDate(), output.totalExpense(), output.categories().stream().map(CategoryAmountResponse::from).toList());
    }

    public record CategoryAmountResponse(String categoryId, String categoryName, BigDecimal amount, BigDecimal percentage) {

        public static CategoryAmountResponse from(CategoryAmount category) {
            return new CategoryAmountResponse(category.categoryId(), category.categoryName(), category.amount(), category.percentage());
        }
    }
}
