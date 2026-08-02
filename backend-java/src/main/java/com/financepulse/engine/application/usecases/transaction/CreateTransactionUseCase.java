package com.financepulse.engine.application.usecases.transaction;

import com.financepulse.engine.application.ports.AccountRepository;
import com.financepulse.engine.application.ports.CategoryRepository;
import com.financepulse.engine.application.ports.IdGenerator;
import com.financepulse.engine.application.ports.TransactionRepository;
import com.financepulse.engine.domain.account.Account;
import com.financepulse.engine.domain.account.errors.AccountNotFoundException;
import com.financepulse.engine.domain.account.errors.ArchivedAccountException;
import com.financepulse.engine.domain.category.errors.CategoryNotFoundException;
import com.financepulse.engine.domain.transaction.Transaction;
import com.financepulse.engine.domain.transaction.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class CreateTransactionUseCase {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final IdGenerator idGenerator;

    public CreateTransactionUseCase(
            TransactionRepository transactionRepository,
            AccountRepository accountRepository,
            CategoryRepository categoryRepository,
            IdGenerator idGenerator) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.categoryRepository = categoryRepository;
        this.idGenerator = idGenerator;
    }

    public Output execute(Input input) {
        Account account = accountRepository
                .findByIdAndUserId(input.accountId(), input.userId())
                .orElseThrow(AccountNotFoundException::new);
        if (account.isArchived()) {
            throw new ArchivedAccountException();
        }
        categoryRepository
                .findByIdAndUserId(input.categoryId(), input.userId())
                .orElseThrow(CategoryNotFoundException::new);

        Transaction transaction = Transaction.create(
                idGenerator.generate(),
                input.userId(),
                input.accountId(),
                input.categoryId(),
                input.type(),
                input.amount(),
                input.date(),
                input.description(),
                input.tags());

        transactionRepository.save(transaction);

        return new Output(transaction.getId());
    }

    public record Input(
            String userId,
            String accountId,
            String categoryId,
            TransactionType type,
            BigDecimal amount,
            LocalDate date,
            String description,
            List<String> tags) {
    }

    public record Output(String transactionId) {
    }
}
