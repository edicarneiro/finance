package com.financepulse.engine.adapters.in.web.dto;

import com.financepulse.engine.domain.budget.BudgetPeriodType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CreateBudgetRequest(
        @NotBlank String categoryId,
        @NotNull BigDecimal limitAmount,
        @NotNull BudgetPeriodType periodType,
        LocalDate customPeriodStart,
        LocalDate customPeriodEnd,
        List<Integer> alertThresholds) {
}
