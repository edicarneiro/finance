package com.financepulse.engine.adapters.in.web.dto;

import com.financepulse.engine.domain.account.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CreateAccountRequest(
        @NotNull AccountType type,
        @NotBlank String name,
        @NotBlank String currency,
        @NotNull BigDecimal initialBalance) {
}
