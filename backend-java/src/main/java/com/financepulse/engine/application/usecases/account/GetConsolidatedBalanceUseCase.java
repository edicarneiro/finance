package com.financepulse.engine.application.usecases.account;

import com.financepulse.engine.application.ports.AccountRepository;
import com.financepulse.engine.application.ports.TransactionRepository;
import com.financepulse.engine.application.services.AccountBalanceCalculator;
import java.math.BigDecimal;

/**
 * RF-012: soma o saldo atual (RN-001) de todas as contas ativas (não
 * arquivadas) em um único total. Sem agrupamento por moeda — vision.md assume
 * operação em moeda única (BRL) para o MVP; multi-moeda é Pós-MVP (ADR-0014).
 */
public class GetConsolidatedBalanceUseCase {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public GetConsolidatedBalanceUseCase(AccountRepository accountRepository, TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    public Output execute(Input input) {
        BigDecimal total = accountRepository.findAllByUserId(input.userId()).stream()
                .filter(account -> !account.isArchived())
                .map(account -> AccountBalanceCalculator.currentBalance(
                        account, transactionRepository.findAllByAccountIdAndUserId(account.getId(), input.userId())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new Output(total);
    }

    public record Input(String userId) {
    }

    public record Output(BigDecimal consolidatedBalance) {
    }
}
