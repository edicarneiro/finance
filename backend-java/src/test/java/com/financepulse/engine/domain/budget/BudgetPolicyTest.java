package com.financepulse.engine.domain.budget;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.financepulse.engine.domain.budget.errors.InvalidAlertThresholdException;
import com.financepulse.engine.domain.budget.errors.InvalidBudgetLimitException;
import com.financepulse.engine.domain.budget.errors.InvalidBudgetPeriodException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class BudgetPolicyTest {

    @Test
    void acceptsAPositiveLimit() {
        assertThatCode(() -> BudgetPolicy.assertPositiveLimit(new BigDecimal("100.00"))).doesNotThrowAnyException();
    }

    @Test
    void rejectsANonPositiveLimit() {
        assertThatThrownBy(() -> BudgetPolicy.assertPositiveLimit(BigDecimal.ZERO)).isInstanceOf(InvalidBudgetLimitException.class);
    }

    @Test
    void returnsDefaultThresholdsWhenNoneProvided() {
        assertThat(BudgetPolicy.assertValidThresholds(null)).isEqualTo(BudgetPolicy.DEFAULT_ALERT_THRESHOLDS);
        assertThat(BudgetPolicy.assertValidThresholds(List.of())).isEqualTo(BudgetPolicy.DEFAULT_ALERT_THRESHOLDS);
    }

    @Test
    void acceptsValidThresholds() {
        assertThat(BudgetPolicy.assertValidThresholds(List.of(50, 90))).containsExactly(50, 90);
    }

    @Test
    void rejectsAThresholdOutOfRange() {
        assertThatThrownBy(() -> BudgetPolicy.assertValidThresholds(List.of(0)))
                .isInstanceOf(InvalidAlertThresholdException.class);
        assertThatThrownBy(() -> BudgetPolicy.assertValidThresholds(List.of(101)))
                .isInstanceOf(InvalidAlertThresholdException.class);
    }

    @Test
    void acceptsACustomPeriodWithAValidRange() {
        assertThatCode(() -> BudgetPolicy.assertValidPeriod(BudgetPeriodType.CUSTOM, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsACustomPeriodWithoutDates() {
        assertThatThrownBy(() -> BudgetPolicy.assertValidPeriod(BudgetPeriodType.CUSTOM, null, null))
                .isInstanceOf(InvalidBudgetPeriodException.class);
    }

    @Test
    void rejectsACustomPeriodWithStartNotBeforeEnd() {
        assertThatThrownBy(() -> BudgetPolicy.assertValidPeriod(BudgetPeriodType.CUSTOM, LocalDate.of(2026, 1, 31), LocalDate.of(2026, 1, 1)))
                .isInstanceOf(InvalidBudgetPeriodException.class);
    }

    @Test
    void rejectsCustomDatesOnAMonthlyOrWeeklyBudget() {
        assertThatThrownBy(() -> BudgetPolicy.assertValidPeriod(BudgetPeriodType.MONTHLY, LocalDate.now(), null))
                .isInstanceOf(InvalidBudgetPeriodException.class);
        assertThatThrownBy(() -> BudgetPolicy.assertValidPeriod(BudgetPeriodType.WEEKLY, null, LocalDate.now()))
                .isInstanceOf(InvalidBudgetPeriodException.class);
    }

    @Test
    void acceptsAMonthlyOrWeeklyBudgetWithoutCustomDates() {
        assertThatCode(() -> BudgetPolicy.assertValidPeriod(BudgetPeriodType.MONTHLY, null, null)).doesNotThrowAnyException();
        assertThatCode(() -> BudgetPolicy.assertValidPeriod(BudgetPeriodType.WEEKLY, null, null)).doesNotThrowAnyException();
    }
}
