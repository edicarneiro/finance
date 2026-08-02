package com.financepulse.engine.application.services;

import static org.assertj.core.api.Assertions.assertThat;

import com.financepulse.engine.application.services.SpendingByCategoryCalculator.CategoryAmount;
import com.financepulse.engine.application.services.SpendingByCategoryCalculator.Result;
import com.financepulse.engine.domain.transaction.Transaction;
import com.financepulse.engine.domain.transaction.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class SpendingByCategoryCalculatorTest {

    @Test
    void groupsExpensesByCategoryAndComputesPercentageOfTheTotal() {
        List<Transaction> transactions = List.of(
                transaction(TransactionType.EXPENSE, "cat-food", "300"),
                transaction(TransactionType.EXPENSE, "cat-food", "100"),
                transaction(TransactionType.EXPENSE, "cat-transport", "100"));

        Result result = SpendingByCategoryCalculator.calculate(transactions);

        assertThat(result.totalExpense()).isEqualByComparingTo("500");
        assertThat(result.categories()).extracting(CategoryAmount::categoryId).containsExactly("cat-food", "cat-transport");
        assertThat(result.categories().get(0).amount()).isEqualByComparingTo("400");
        assertThat(result.categories().get(0).percentage()).isEqualByComparingTo("80.0000");
        assertThat(result.categories().get(1).amount()).isEqualByComparingTo("100");
        assertThat(result.categories().get(1).percentage()).isEqualByComparingTo("20.0000");
    }

    @Test
    void ignoresIncomeTransactions() {
        List<Transaction> transactions =
                List.of(transaction(TransactionType.INCOME, "cat-salary", "1000"), transaction(TransactionType.EXPENSE, "cat-food", "50"));

        Result result = SpendingByCategoryCalculator.calculate(transactions);

        assertThat(result.totalExpense()).isEqualByComparingTo("50");
        assertThat(result.categories()).extracting(CategoryAmount::categoryId).containsExactly("cat-food");
    }

    @Test
    void returnsAnEmptyResultWithoutDivisionByZeroWhenThereAreNoExpenses() {
        Result result = SpendingByCategoryCalculator.calculate(List.of());

        assertThat(result.totalExpense()).isEqualByComparingTo("0");
        assertThat(result.categories()).isEmpty();
    }

    private static Transaction transaction(TransactionType type, String categoryId, String amount) {
        return Transaction.create(
                "tx-" + categoryId + "-" + amount, "user-1", "account-1", categoryId, type, new BigDecimal(amount), LocalDate.of(2026, 7, 15), null,
                List.of());
    }
}
