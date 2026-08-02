package com.financepulse.engine.application.usecases.transaction;

import com.financepulse.engine.application.ports.AccountRepository;
import com.financepulse.engine.application.ports.TransactionRepository;
import com.financepulse.engine.domain.account.errors.AccountNotFoundException;
import com.financepulse.engine.domain.transaction.Transaction;
import com.financepulse.engine.domain.transaction.TransactionType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Listagem mínima por conta — filtro/busca completo (RF-018) fica para a Fase 4.3 (ver ADR-0016). */
public class ListTransactionsUseCase {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    public ListTransactionsUseCase(TransactionRepository transactionRepository, AccountRepository accountRepository) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
    }

    public Output execute(Input input) {
        accountRepository.findByIdAndUserId(input.accountId(), input.userId()).orElseThrow(AccountNotFoundException::new);

        List<TransactionView> transactions = transactionRepository
                .findAllByAccountIdAndUserId(input.accountId(), input.userId())
                .stream()
                .map(this::toView)
                .toList();

        return new Output(transactions);
    }

    private TransactionView toView(Transaction transaction) {
        return new TransactionView(
                transaction.getId(),
                transaction.getAccountId(),
                transaction.getCategoryId(),
                transaction.getType(),
                transaction.getAmount(),
                transaction.getDate(),
                transaction.getDescription(),
                transaction.getTags(),
                transaction.getCreatedAt());
    }

    public record Input(String userId, String accountId) {
    }

    public record Output(List<TransactionView> transactions) {
    }

    public record TransactionView(
            String id,
            String accountId,
            String categoryId,
            TransactionType type,
            BigDecimal amount,
            LocalDate date,
            String description,
            List<String> tags,
            Instant createdAt) {
    }
}
