package com.financepulse.engine.application.usecases.account;

import com.financepulse.engine.application.ports.AccountRepository;
import com.financepulse.engine.domain.account.Account;
import com.financepulse.engine.domain.account.errors.AccountNotFoundException;

/** RF-010/RF-013: única forma de remoção de conta nesta fase (ver ADR-0014). Idempotente. */
public class ArchiveAccountUseCase {

    private final AccountRepository accountRepository;

    public ArchiveAccountUseCase(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public void execute(Input input) {
        Account account = accountRepository
                .findByIdAndUserId(input.accountId(), input.userId())
                .orElseThrow(AccountNotFoundException::new);

        accountRepository.update(account.archive());
    }

    public record Input(String userId, String accountId) {
    }
}
