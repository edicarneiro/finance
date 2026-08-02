package com.financepulse.engine.adapters.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CreateGoalRequest(
        @NotBlank String name,
        @NotNull BigDecimal targetAmount,
        @NotNull LocalDate deadline,
        String accountId,
        String categoryId,
        List<Integer> progressAlertThresholds) {
}
