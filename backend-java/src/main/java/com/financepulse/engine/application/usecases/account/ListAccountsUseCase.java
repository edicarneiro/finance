package com.financepulse.engine.application.usecases.account;

import com.financepulse.engine.application.ports.AccountRepository;
import com.financepulse.engine.application.ports.TransactionRepository;
import com.financepulse.engine.application.services.AccountBalanceCalculator;
import com.financepulse.engine.domain.account.Account;
import com.financepulse.engine.domain.account.AccountType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** RF-011: cada conta listada traz seu saldo atual, derivado de suas transações (RN-001, ver ADR-0016). */
public class ListAccountsUseCase {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public ListAccountsUseCase(AccountRepository accountRepository, TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    public Output execute(Input input) {
        List<AccountView> accounts = accountRepository.findAllByUserId(input.userId()).stream()
                .map(account -> toView(account, input.userId()))
                .toList();

        return new Output(accounts);
    }

    private AccountView toView(Account account, String userId) {
        BigDecimal currentBalance = AccountBalanceCalculator.currentBalance(
                account, transactionRepository.findAllByAccountIdAndUserId(account.getId(), userId));

        return new AccountView(
                account.getId(),
                account.getType(),
                account.getName(),
                account.getCurrency().toString(),
                currentBalance,
                account.isArchived(),
                account.getCreatedAt());
    }

    public record Input(String userId) {
    }

    public record Output(List<AccountView> accounts) {
    }

    public record AccountView(
            String id,
            AccountType type,
            String name,
            String currency,
            BigDecimal balance,
            boolean archived,
            Instant createdAt) {
    }
}
