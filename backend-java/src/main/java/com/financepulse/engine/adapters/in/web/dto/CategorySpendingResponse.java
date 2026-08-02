package com.financepulse.engine.adapters.in.web.dto;

import com.financepulse.engine.application.usecases.dashboard.GetDashboardUseCase.CategorySpending;
import java.math.BigDecimal;

public record CategorySpendingResponse(String categoryId, String categoryName, BigDecimal amount, BigDecimal percentage) {

    public static CategorySpendingResponse from(CategorySpending spending) {
        return new CategorySpendingResponse(spending.categoryId(), spending.categoryName(), spending.amount(), spending.percentage());
    }
}
