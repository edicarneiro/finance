package com.financepulse.engine.adapters.in.web.dto;

import com.financepulse.engine.application.usecases.transaction.ListTransactionsUseCase.TransactionView;
import com.financepulse.engine.domain.transaction.TransactionType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record TransactionResponse(
        String id,
        String accountId,
        String categoryId,
        TransactionType type,
        BigDecimal amount,
        LocalDate date,
        String description,
        List<String> tags,
        Instant createdAt) {

    public static TransactionResponse from(TransactionView view) {
        return new TransactionResponse(
                view.id(),
                view.accountId(),
                view.categoryId(),
                view.type(),
                view.amount(),
                view.date(),
                view.description(),
                view.tags(),
                view.createdAt());
    }
}
