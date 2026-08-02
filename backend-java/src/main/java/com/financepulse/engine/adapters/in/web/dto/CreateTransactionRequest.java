package com.financepulse.engine.adapters.in.web.dto;

import com.financepulse.engine.domain.transaction.TransactionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CreateTransactionRequest(
        @NotBlank String accountId,
        @NotBlank String categoryId,
        @NotNull TransactionType type,
        @NotNull BigDecimal amount,
        @NotNull LocalDate date,
        String description,
        List<String> tags) {
}
