package com.financepulse.engine.application.services;

import static org.assertj.core.api.Assertions.assertThat;

import com.financepulse.engine.application.services.PeriodComparisonCalculator.CategoryComparison;
import com.financepulse.engine.application.services.PeriodComparisonCalculator.Result;
import com.financepulse.engine.domain.transaction.Transaction;
import com.financepulse.engine.domain.transaction.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class PeriodComparisonCalculatorTest {

    @Test
    void comparesTotalsBetweenTwoPeriods() {
        List<Transaction> periodA = List.of(transaction(TransactionType.INCOME, "salary", "1000"), transaction(TransactionType.EXPENSE, "food", "300"));
        List<Transaction> periodB = List.of(transaction(TransactionType.INCOME, "salary", "1200"), transaction(TransactionType.EXPENSE, "food", "400"));

        Result result = PeriodComparisonCalculator.calculate(periodA, periodB);

        assertThat(result.periodA().totalIncome()).isEqualByComparingTo("1000");
        assertThat(result.periodA().totalExpense()).isEqualByComparingTo("300");
        assertThat(result.periodA().net()).isEqualByComparingTo("700");
        assertThat(result.periodB().totalIncome()).isEqualByComparingTo("1200");
        assertThat(result.periodB().totalExpense()).isEqualByComparingTo("400");
        assertThat(result.periodB().net()).isEqualByComparingTo("800");
    }

    @Test
    void computesDeltaAndPercentageChangePerCategory() {
        List<Transaction> periodA = List.of(transaction(TransactionType.EXPENSE, "food", "200"));
        List<Transaction> periodB = List.of(transaction(TransactionType.EXPENSE, "food", "300"));

        Result result = PeriodComparisonCalculator.calculate(periodA, periodB);

        CategoryComparison food = result.categoryComparisons().get(0);
        assertThat(food.amountA()).isEqualByComparingTo("200");
        assertThat(food.amountB()).isEqualByComparingTo("300");
        assertThat(food.delta()).isEqualByComparingTo("100");
        assertThat(food.percentageChange()).isEqualByComparingTo("50");
    }

    @Test
    void leavesPercentageChangeNullWhenTheCategoryHadNoSpendingInPeriodA() {
        List<Transaction> periodA = List.of();
        List<Transaction> periodB = List.of(transaction(TransactionType.EXPENSE, "new-category", "80"));

        Result result = PeriodComparisonCalculator.calculate(periodA, periodB);

        CategoryComparison comparison = result.categoryComparisons().get(0);
        assertThat(comparison.amountA()).isEqualByComparingTo("0");
        assertThat(comparison.amountB()).isEqualByComparingTo("80");
        assertThat(comparison.percentageChange()).isNull();
    }

    @Test
    void includesCategoriesPresentInEitherPeriodEvenIfAbsentFromTheOther() {
        List<Transaction> periodA = List.of(transaction(TransactionType.EXPENSE, "transport", "100"));
        List<Transaction> periodB = List.of(transaction(TransactionType.EXPENSE, "food", "300"), transaction(TransactionType.EXPENSE, "transport", "50"));

        Result result = PeriodComparisonCalculator.calculate(periodA, periodB);

        assertThat(result.categoryComparisons()).extracting(CategoryComparison::categoryId).containsExactlyInAnyOrder("food", "transport");
    }

    @Test
    void ignoresIncomeWhenComparingCategories() {
        List<Transaction> periodA = List.of(transaction(TransactionType.INCOME, "salary", "1000"));
        List<Transaction> periodB = List.of(transaction(TransactionType.INCOME, "salary", "1200"));

        Result result = PeriodComparisonCalculator.calculate(periodA, periodB);

        assertThat(result.categoryComparisons()).isEmpty();
    }

    private static Transaction transaction(TransactionType type, String categoryId, String amount) {
        return Transaction.create(
                "tx-" + categoryId + "-" + amount + "-" + type, "user-1", "account-1", categoryId, type, new BigDecimal(amount),
                LocalDate.of(2026, 7, 15), null, List.of());
    }
}
