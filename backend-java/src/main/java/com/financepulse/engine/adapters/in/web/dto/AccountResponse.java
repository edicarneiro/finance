package com.financepulse.engine.adapters.in.web.dto;

import com.financepulse.engine.application.usecases.account.ListAccountsUseCase.AccountView;
import com.financepulse.engine.domain.account.AccountType;
import java.math.BigDecimal;
import java.time.Instant;

public record AccountResponse(
        String id, AccountType type, String name, String currency, BigDecimal balance, boolean archived, Instant createdAt) {

    public static AccountResponse from(AccountView view) {
        return new AccountResponse(
                view.id(), view.type(), view.name(), view.currency(), view.balance(), view.archived(), view.createdAt());
    }
}
