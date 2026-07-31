package com.financepulse.engine.application.usecases.account;

import com.financepulse.engine.application.ports.AccountRepository;
import com.financepulse.engine.domain.account.Account;
import java.math.BigDecimal;

/**
 * RF-012: soma o saldo de todas as contas ativas (não arquivadas) em um único
 * total. Sem agrupamento por moeda — vision.md assume operação em moeda única
 * (BRL) para o MVP; multi-moeda é Pós-MVP (ver ADR-0014).
 */
public class GetConsolidatedBalanceUseCase {

    private final AccountRepository accountRepository;

    public GetConsolidatedBalanceUseCase(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public Output execute(Input input) {
        BigDecimal total = accountRepository.findAllByUserId(input.userId()).stream()
                .filter(account -> !account.isArchived())
                .map(Account::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new Output(total);
    }

    public record Input(String userId) {
    }

    public record Output(BigDecimal consolidatedBalance) {
    }
}
