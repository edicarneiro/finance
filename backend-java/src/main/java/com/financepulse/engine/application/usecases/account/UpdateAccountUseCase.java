package com.financepulse.engine.application.usecases.account;

import com.financepulse.engine.application.ports.AccountRepository;
import com.financepulse.engine.domain.account.Account;
import com.financepulse.engine.domain.account.errors.AccountNotFoundException;

/**
 * RF-010: apenas o nome é editável (ver ADR-0014). Uma conta de outro usuário
 * é tratada como "não encontrada" (mesmo erro/HTTP status de uma conta
 * inexistente) — postura anti-enumeração consistente com o restante do
 * projeto (ex.: login), e reforço de RF-047.
 */
public class UpdateAccountUseCase {

    private final AccountRepository accountRepository;

    public UpdateAccountUseCase(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public void execute(Input input) {
        Account account = accountRepository
                .findByIdAndUserId(input.accountId(), input.userId())
                .orElseThrow(AccountNotFoundException::new);

        accountRepository.update(account.withName(input.name()));
    }

    public record Input(String userId, String accountId, String name) {
    }
}
