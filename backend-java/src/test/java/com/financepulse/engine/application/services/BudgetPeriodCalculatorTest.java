package com.financepulse.engine.application.services;

import static org.assertj.core.api.Assertions.assertThat;

import com.financepulse.engine.application.services.BudgetPeriodCalculator.PeriodRange;
import com.financepulse.engine.domain.budget.Budget;
import com.financepulse.engine.domain.budget.BudgetPeriodType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class BudgetPeriodCalculatorTest {

    @Test
    void currentPeriodForAMonthlyBudgetIsTheFullCalendarMonth() {
        Budget budget = Budget.create("budget-1", "user-1", "category-1", BigDecimal.TEN, BudgetPeriodType.MONTHLY, null, null, null);

        PeriodRange period = BudgetPeriodCalculator.currentPeriod(budget, LocalDate.of(2026, 7, 15));

        assertThat(period.start()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(period.end()).isEqualTo(LocalDate.of(2026, 7, 31));
    }

    @Test
    void currentPeriodForAWeeklyBudgetIsMondayToSunday() {
        Budget budget = Budget.create("budget-1", "user-1", "category-1", BigDecimal.TEN, BudgetPeriodType.WEEKLY, null, null, null);

        // 2026-07-31 is a Friday.
        PeriodRange period = BudgetPeriodCalculator.currentPeriod(budget, LocalDate.of(2026, 7, 31));

        assertThat(period.start()).isEqualTo(LocalDate.of(2026, 7, 27));
        assertThat(period.end()).isEqualTo(LocalDate.of(2026, 8, 2));
    }

    @Test
    void currentPeriodForACustomBudgetIsTheStoredRange() {
        Budget budget = Budget.create(
                "budget-1",
                "user-1",
                "category-1",
                BigDecimal.TEN,
                BudgetPeriodType.CUSTOM,
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 15),
                null);

        PeriodRange period = BudgetPeriodCalculator.currentPeriod(budget, LocalDate.of(2026, 7, 31));

        assertThat(period.start()).isEqualTo(LocalDate.of(2026, 3, 1));
        assertThat(period.end()).isEqualTo(LocalDate.of(2026, 3, 15));
    }

    @Test
    void previousPeriodsForAMonthlyBudgetGoBackByCalendarMonth() {
        Budget budget = Budget.create("budget-1", "user-1", "category-1", BigDecimal.TEN, BudgetPeriodType.MONTHLY, null, null, null);

        List<PeriodRange> previous = BudgetPeriodCalculator.previousPeriods(budget, LocalDate.of(2026, 7, 15), 2);

        assertThat(previous).containsExactly(
                new PeriodRange(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30)),
                new PeriodRange(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31)));
    }

    @Test
    void previousPeriodsForACustomBudgetIsAlwaysEmpty() {
        Budget budget = Budget.create(
                "budget-1",
                "user-1",
                "category-1",
                BigDecimal.TEN,
                BudgetPeriodType.CUSTOM,
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 15),
                null);

        assertThat(BudgetPeriodCalculator.previousPeriods(budget, LocalDate.of(2026, 7, 31), 6)).isEmpty();
    }

    @Test
    void periodRangeContainsIncludesBothEndpoints() {
        PeriodRange range = new PeriodRange(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31));

        assertThat(range.contains(LocalDate.of(2026, 7, 1))).isTrue();
        assertThat(range.contains(LocalDate.of(2026, 7, 31))).isTrue();
        assertThat(range.contains(LocalDate.of(2026, 6, 30))).isFalse();
        assertThat(range.contains(LocalDate.of(2026, 8, 1))).isFalse();
    }
}
