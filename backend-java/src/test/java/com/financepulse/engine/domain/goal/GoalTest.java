package com.financepulse.engine.domain.goal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.financepulse.engine.domain.goal.errors.InvalidGoalAssociationException;
import com.financepulse.engine.domain.goal.errors.InvalidGoalTargetException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class GoalTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 7, 31);

    @Test
    void createsAnAccountBasedGoalWithDefaultThresholds() {
        Goal goal = Goal.create(
                "goal-1", "user-1", "Reserva de emergência", new BigDecimal("10000.00"), LocalDate.of(2026, 12, 31), "account-1", null, null, TODAY);

        assertThat(goal.isAccountBased()).isTrue();
        assertThat(goal.getAccountId()).contains("account-1");
        assertThat(goal.getCategoryId()).isEmpty();
        assertThat(goal.getProgressAlertThresholds()).isEqualTo(GoalPolicy.DEFAULT_ALERT_THRESHOLDS);
    }

    @Test
    void createsACategoryBasedGoal() {
        Goal goal = Goal.create(
                "goal-1", "user-1", "Viagem", new BigDecimal("5000.00"), LocalDate.of(2026, 12, 31), null, "category-1", List.of(50), TODAY);

        assertThat(goal.isAccountBased()).isFalse();
        assertThat(goal.getCategoryId()).contains("category-1");
        assertThat(goal.getProgressAlertThresholds()).containsExactly(50);
    }

    @Test
    void rejectsCreationWithBothAssociations() {
        assertThatThrownBy(() -> Goal.create(
                        "goal-1", "user-1", "Meta", BigDecimal.TEN, LocalDate.of(2026, 12, 31), "account-1", "category-1", null, TODAY))
                .isInstanceOf(InvalidGoalAssociationException.class);
    }

    @Test
    void editingDetailsPreservesTheAssociation() {
        Goal original = Goal.create(
                "goal-1", "user-1", "Reserva", new BigDecimal("1000.00"), LocalDate.of(2026, 12, 31), "account-1", null, null, TODAY);

        Goal edited = original.withDetails("Reserva de emergência", new BigDecimal("2000.00"), LocalDate.of(2027, 1, 31), List.of(90), TODAY);

        assertThat(edited.getName()).isEqualTo("Reserva de emergência");
        assertThat(edited.getTargetAmount()).isEqualByComparingTo("2000.00");
        assertThat(edited.getAccountId()).isEqualTo(original.getAccountId());
        assertThat(edited.getId()).isEqualTo(original.getId());
    }

    @Test
    void rejectsEditingToANonPositiveTarget() {
        Goal goal = Goal.create("goal-1", "user-1", "Reserva", new BigDecimal("1000.00"), LocalDate.of(2026, 12, 31), "account-1", null, null, TODAY);

        assertThatThrownBy(() -> goal.withDetails("Reserva", BigDecimal.ZERO, LocalDate.of(2027, 1, 31), null, TODAY))
                .isInstanceOf(InvalidGoalTargetException.class);
    }
}
