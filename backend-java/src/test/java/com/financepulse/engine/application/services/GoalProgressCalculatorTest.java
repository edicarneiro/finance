package com.financepulse.engine.application.services;

import static org.assertj.core.api.Assertions.assertThat;

import com.financepulse.engine.application.services.GoalProgressCalculator.Progress;
import com.financepulse.engine.domain.goal.Goal;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class GoalProgressCalculatorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 7, 31);

    private final Goal goal = Goal.create(
            "goal-1", "user-1", "Reserva", new BigDecimal("1000.00"), LocalDate.of(2026, 12, 31), "account-1", null, List.of(50, 80, 100), TODAY);

    @Test
    void calculatesPercentageAndCrossedThresholds() {
        Progress progress = GoalProgressCalculator.calculate(goal, new BigDecimal("850.00"), TODAY);

        assertThat(progress.percentage()).isEqualByComparingTo("85.0000");
        assertThat(progress.thresholdsCrossed()).containsExactly(50, 80);
        assertThat(progress.achieved()).isFalse();
        assertThat(progress.overdue()).isFalse();
    }

    @Test
    void marksAsAchievedWhenCurrentAmountReachesTarget() {
        Progress progress = GoalProgressCalculator.calculate(goal, new BigDecimal("1000.00"), TODAY);

        assertThat(progress.achieved()).isTrue();
        assertThat(progress.thresholdsCrossed()).containsExactly(50, 80, 100);
    }

    @Test
    void marksAsAchievedWhenCurrentAmountExceedsTarget() {
        Progress progress = GoalProgressCalculator.calculate(goal, new BigDecimal("1200.00"), TODAY);

        assertThat(progress.achieved()).isTrue();
        assertThat(progress.percentage()).isEqualByComparingTo("120.0000");
    }

    @Test
    void marksAsOverdueWhenPastDeadlineAndNotAchieved() {
        Progress progress = GoalProgressCalculator.calculate(goal, new BigDecimal("500.00"), LocalDate.of(2027, 1, 1));

        assertThat(progress.overdue()).isTrue();
    }

    @Test
    void doesNotMarkAsOverdueWhenAchievedEvenIfPastDeadline() {
        Progress progress = GoalProgressCalculator.calculate(goal, new BigDecimal("1000.00"), LocalDate.of(2027, 1, 1));

        assertThat(progress.overdue()).isFalse();
    }

    @Test
    void handlesANegativeCurrentAmountForCategoryBasedGoals() {
        Progress progress = GoalProgressCalculator.calculate(goal, new BigDecimal("-50.00"), TODAY);

        assertThat(progress.percentage()).isEqualByComparingTo("-5.0000");
        assertThat(progress.thresholdsCrossed()).isEmpty();
        assertThat(progress.achieved()).isFalse();
    }
}
