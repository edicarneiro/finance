package com.financepulse.engine.domain.budget;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.financepulse.engine.domain.budget.errors.InvalidBudgetLimitException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class BudgetTest {

    @Test
    void createsAMonthlyBudgetWithDefaultThresholds() {
        Budget budget = Budget.create(
                "budget-1", "user-1", "category-1", new BigDecimal("500.00"), BudgetPeriodType.MONTHLY, null, null, null);

        assertThat(budget.getLimitAmount()).isEqualByComparingTo("500.00");
        assertThat(budget.getPeriodType()).isEqualTo(BudgetPeriodType.MONTHLY);
        assertThat(budget.getAlertThresholds()).isEqualTo(BudgetPolicy.DEFAULT_ALERT_THRESHOLDS);
        assertThat(budget.getCustomPeriodStart()).isEmpty();
    }

    @Test
    void createsACustomBudgetWithAnExplicitRange() {
        Budget budget = Budget.create(
                "budget-1",
                "user-1",
                "category-1",
                new BigDecimal("200.00"),
                BudgetPeriodType.CUSTOM,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 31),
                List.of(90));

        assertThat(budget.getCustomPeriodStart()).contains(LocalDate.of(2026, 1, 1));
        assertThat(budget.getCustomPeriodEnd()).contains(LocalDate.of(2026, 1, 31));
        assertThat(budget.getAlertThresholds()).containsExactly(90);
    }

    @Test
    void rejectsCreationWithANonPositiveLimit() {
        assertThatThrownBy(() -> Budget.create("budget-1", "user-1", "category-1", BigDecimal.ZERO, BudgetPeriodType.MONTHLY, null, null, null))
                .isInstanceOf(InvalidBudgetLimitException.class);
    }

    @Test
    void editingLimitAndThresholdsPreservesCategoryAndPeriodType() {
        Budget original = Budget.create(
                "budget-1", "user-1", "category-1", new BigDecimal("500.00"), BudgetPeriodType.MONTHLY, null, null, null);

        Budget edited = original.withLimitAndThresholds(new BigDecimal("600.00"), List.of(50));

        assertThat(edited.getLimitAmount()).isEqualByComparingTo("600.00");
        assertThat(edited.getAlertThresholds()).containsExactly(50);
        assertThat(edited.getCategoryId()).isEqualTo(original.getCategoryId());
        assertThat(edited.getPeriodType()).isEqualTo(original.getPeriodType());
        assertThat(edited.getId()).isEqualTo(original.getId());
    }

    @Test
    void rejectsEditingToANonPositiveLimit() {
        Budget budget = Budget.create("budget-1", "user-1", "category-1", new BigDecimal("500.00"), BudgetPeriodType.MONTHLY, null, null, null);

        assertThatThrownBy(() -> budget.withLimitAndThresholds(BigDecimal.ZERO, null)).isInstanceOf(InvalidBudgetLimitException.class);
    }
}
