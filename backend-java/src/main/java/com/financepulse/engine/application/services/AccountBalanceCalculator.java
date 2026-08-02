package com.financepulse.engine.application.services;

import com.financepulse.engine.domain.account.Account;
import com.financepulse.engine.domain.transaction.Transaction;
import com.financepulse.engine.domain.transaction.TransactionType;
import java.math.BigDecimal;
import java.util.List;

/**
 * RF-011/RN-001 (ver ADR-0016): o saldo armazenado em {@link Account} representa
 * exclusivamente o saldo inicial; o saldo atual é sempre derivado somando as
 * transações da conta, nunca um campo mutável.
 */
public final class AccountBalanceCalculator {

    private AccountBalanceCalculator() {
    }

    public static BigDecimal currentBalance(Account account, List<Transaction> transactions) {
        BigDecimal net = transactions.stream()
                .map(transaction -> transaction.getType() == TransactionType.INCOME
                        ? transaction.getAmount()
                        : transaction.getAmount().negate())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return account.getBalance().add(net);
    }
}
