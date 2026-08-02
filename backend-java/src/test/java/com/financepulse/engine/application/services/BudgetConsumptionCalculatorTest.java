package com.financepulse.engine.application.services;

import static org.assertj.core.api.Assertions.assertThat;

import com.financepulse.engine.application.services.BudgetConsumptionCalculator.Consumption;
import com.financepulse.engine.application.services.BudgetPeriodCalculator.PeriodRange;
import com.financepulse.engine.domain.budget.Budget;
import com.financepulse.engine.domain.budget.BudgetPeriodType;
import com.financepulse.engine.domain.transaction.Transaction;
import com.financepulse.engine.domain.transaction.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class BudgetConsumptionCalculatorTest {

    private final PeriodRange july = new PeriodRange(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31));

    @Test
    void sumsOnlyExpenseTransactionsWithinThePeriod() {
        Budget budget = Budget.create(
                "budget-1", "user-1", "category-1", new BigDecimal("500.00"), BudgetPeriodType.MONTHLY, null, null, List.of(80, 100));
        List<Transaction> transactions = List.of(
                expense("120.00", LocalDate.of(2026, 7, 10)),
                expense("80.00", LocalDate.of(2026, 7, 20)),
                income("999.00", LocalDate.of(2026, 7, 15)),
                expense("50.00", LocalDate.of(2026, 6, 30)));

        Consumption consumption = BudgetConsumptionCalculator.calculate(budget, july, transactions);

        assertThat(consumption.consumedAmount()).isEqualByComparingTo("200.00");
        assertThat(consumption.percentage()).isEqualByComparingTo("40.0000");
        assertThat(consumption.thresholdsCrossed()).isEmpty();
    }

    @Test
    void identifiesCrossedThresholds() {
        Budget budget = Budget.create(
                "budget-1", "user-1", "category-1", new BigDecimal("100.00"), BudgetPeriodType.MONTHLY, null, null, List.of(50, 80, 100));
        List<Transaction> transactions = List.of(expense("85.00", LocalDate.of(2026, 7, 10)));

        Consumption consumption = BudgetConsumptionCalculator.calculate(budget, july, transactions);

        assertThat(consumption.percentage()).isEqualByComparingTo("85.0000");
        assertThat(consumption.thresholdsCrossed()).containsExactly(50, 80);
    }

    @Test
    void identifiesAllThresholdsCrossedWhenOverBudget() {
        Budget budget = Budget.create(
                "budget-1", "user-1", "category-1", new BigDecimal("100.00"), BudgetPeriodType.MONTHLY, null, null, List.of(80, 100));
        List<Transaction> transactions = List.of(expense("150.00", LocalDate.of(2026, 7, 10)));

        Consumption consumption = BudgetConsumptionCalculator.calculate(budget, july, transactions);

        assertThat(consumption.percentage()).isEqualByComparingTo("150.0000");
        assertThat(consumption.thresholdsCrossed()).containsExactly(80, 100);
    }

    @Test
    void returnsZeroConsumptionWhenThereAreNoMatchingTransactions() {
        Budget budget = Budget.create("budget-1", "user-1", "category-1", new BigDecimal("100.00"), BudgetPeriodType.MONTHLY, null, null, null);

        Consumption consumption = BudgetConsumptionCalculator.calculate(budget, july, List.of());

        assertThat(consumption.consumedAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(consumption.percentage()).isEqualByComparingTo("0.0000");
        assertThat(consumption.thresholdsCrossed()).isEmpty();
    }

    private Transaction expense(String amount, LocalDate date) {
        return Transaction.create("tx", "user-1", "account-1", "category-1", TransactionType.EXPENSE, new BigDecimal(amount), date, null, null);
    }

    private Transaction income(String amount, LocalDate date) {
        return Transaction.create("tx", "user-1", "account-1", "category-1", TransactionType.INCOME, new BigDecimal(amount), date, null, null);
    }
}
