package com.financepulse.engine.application.usecases.account;

import com.financepulse.engine.application.ports.AccountRepository;
import com.financepulse.engine.domain.account.Account;
import com.financepulse.engine.domain.account.AccountType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** RF-011: cada conta listada traz seu saldo atual (nesta fase, igual ao saldo inicial — ver ADR-0014). */
public class ListAccountsUseCase {

    private final AccountRepository accountRepository;

    public ListAccountsUseCase(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public Output execute(Input input) {
        List<AccountView> accounts = accountRepository.findAllByUserId(input.userId()).stream()
                .map(ListAccountsUseCase::toView)
                .toList();

        return new Output(accounts);
    }

    private static AccountView toView(Account account) {
        return new AccountView(
                account.getId(),
                account.getType(),
                account.getName(),
                account.getCurrency().toString(),
                account.getBalance(),
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
