package com.financepulse.engine.application.usecases.transaction;

import com.financepulse.engine.application.ports.AccountRepository;
import com.financepulse.engine.application.ports.CategoryRepository;
import com.financepulse.engine.application.ports.TransactionRepository;
import com.financepulse.engine.domain.account.Account;
import com.financepulse.engine.domain.account.errors.AccountNotFoundException;
import com.financepulse.engine.domain.account.errors.ArchivedAccountException;
import com.financepulse.engine.domain.category.errors.CategoryNotFoundException;
import com.financepulse.engine.domain.transaction.Transaction;
import com.financepulse.engine.domain.transaction.TransactionType;
import com.financepulse.engine.domain.transaction.errors.TransactionNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * RF-015: edição — todos os campos são substituídos (não é um PATCH parcial).
 *
 * <p>Uma conta arquivada só bloqueia a edição quando a transação está sendo
 * <em>movida para</em> ela — corrigir um campo (valor, categoria, descrição)
 * de uma transação que já pertencia a uma conta antes dela ser arquivada
 * continua permitido, já que não é um novo lançamento (ver ADR-0016: a
 * restrição de conta arquivada se aplica à criação de lançamentos, não à
 * correção de histórico já existente).
 */
public class UpdateTransactionUseCase {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;

    public UpdateTransactionUseCase(
            TransactionRepository transactionRepository, AccountRepository accountRepository, CategoryRepository categoryRepository) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.categoryRepository = categoryRepository;
    }

    public void execute(Input input) {
        Transaction transaction = transactionRepository
                .findByIdAndUserId(input.transactionId(), input.userId())
                .orElseThrow(TransactionNotFoundException::new);

        Account account = accountRepository
                .findByIdAndUserId(input.accountId(), input.userId())
                .orElseThrow(AccountNotFoundException::new);
        boolean movingToADifferentAccount = !input.accountId().equals(transaction.getAccountId());
        if (account.isArchived() && movingToADifferentAccount) {
            throw new ArchivedAccountException();
        }
        categoryRepository
                .findByIdAndUserId(input.categoryId(), input.userId())
                .orElseThrow(CategoryNotFoundException::new);

        Transaction updated = transaction.withDetails(
                input.accountId(), input.categoryId(), input.type(), input.amount(), input.date(), input.description(), input.tags());

        transactionRepository.update(updated);
    }

    public record Input(
            String userId,
            String transactionId,
            String accountId,
            String categoryId,
            TransactionType type,
            BigDecimal amount,
            LocalDate date,
            String description,
            List<String> tags) {
    }
}
