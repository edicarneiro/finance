package com.financepulse.engine.application.services;

import static org.assertj.core.api.Assertions.assertThat;

import com.financepulse.engine.domain.account.Account;
import com.financepulse.engine.domain.account.AccountType;
import com.financepulse.engine.domain.account.Currency;
import com.financepulse.engine.domain.transaction.Transaction;
import com.financepulse.engine.domain.transaction.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class AccountBalanceCalculatorTest {

    private final Account account = Account.create(
            "account-1", "user-1", AccountType.CHECKING, "Conta Corrente", Currency.create("BRL"), new BigDecimal("100.00"));

    @Test
    void equalsTheInitialBalanceWhenThereAreNoTransactions() {
        BigDecimal balance = AccountBalanceCalculator.currentBalance(account, List.of());

        assertThat(balance).isEqualByComparingTo("100.00");
    }

    @Test
    void addsIncomeAndSubtractsExpense() {
        Transaction income = Transaction.create(
                "tx-1", "user-1", "account-1", "category-1", TransactionType.INCOME, new BigDecimal("500.00"), LocalDate.now(), null, null);
        Transaction expense = Transaction.create(
                "tx-2", "user-1", "account-1", "category-1", TransactionType.EXPENSE, new BigDecimal("150.00"), LocalDate.now(), null, null);

        BigDecimal balance = AccountBalanceCalculator.currentBalance(account, List.of(income, expense));

        assertThat(balance).isEqualByComparingTo("450.00");
    }

    @Test
    void canGoNegativeWhenExpensesExceedIncome() {
        Transaction expense = Transaction.create(
                "tx-1", "user-1", "account-1", "category-1", TransactionType.EXPENSE, new BigDecimal("500.00"), LocalDate.now(), null, null);

        BigDecimal balance = AccountBalanceCalculator.currentBalance(account, List.of(expense));

        assertThat(balance).isEqualByComparingTo("-400.00");
    }
}
