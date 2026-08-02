package com.financepulse.engine.adapters.in.web.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public record UpdateBudgetRequest(@NotNull BigDecimal limitAmount, List<Integer> alertThresholds) {
}
