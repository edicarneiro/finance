package com.financepulse.engine.domain.goal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.financepulse.engine.domain.goal.errors.InvalidGoalAssociationException;
import com.financepulse.engine.domain.goal.errors.InvalidGoalDeadlineException;
import com.financepulse.engine.domain.goal.errors.InvalidGoalTargetException;
import com.financepulse.engine.domain.goal.errors.InvalidGoalThresholdException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class GoalPolicyTest {

    @Test
    void acceptsAPositiveTarget() {
        assertThatCode(() -> GoalPolicy.assertPositiveTarget(new BigDecimal("100.00"))).doesNotThrowAnyException();
    }

    @Test
    void rejectsANonPositiveTarget() {
        assertThatThrownBy(() -> GoalPolicy.assertPositiveTarget(BigDecimal.ZERO)).isInstanceOf(InvalidGoalTargetException.class);
    }

    @Test
    void acceptsAFutureDeadline() {
        assertThatCode(() -> GoalPolicy.assertFutureDeadline(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 7, 31)))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsATodayOrPastDeadline() {
        assertThatThrownBy(() -> GoalPolicy.assertFutureDeadline(LocalDate.of(2026, 7, 31), LocalDate.of(2026, 7, 31)))
                .isInstanceOf(InvalidGoalDeadlineException.class);
        assertThatThrownBy(() -> GoalPolicy.assertFutureDeadline(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31)))
                .isInstanceOf(InvalidGoalDeadlineException.class);
    }

    @Test
    void acceptsExactlyOneAssociation() {
        assertThatCode(() -> GoalPolicy.assertValidAssociation("account-1", null)).doesNotThrowAnyException();
        assertThatCode(() -> GoalPolicy.assertValidAssociation(null, "category-1")).doesNotThrowAnyException();
    }

    @Test
    void rejectsNeitherAssociation() {
        assertThatThrownBy(() -> GoalPolicy.assertValidAssociation(null, null)).isInstanceOf(InvalidGoalAssociationException.class);
    }

    @Test
    void rejectsBothAssociations() {
        assertThatThrownBy(() -> GoalPolicy.assertValidAssociation("account-1", "category-1"))
                .isInstanceOf(InvalidGoalAssociationException.class);
    }

    @Test
    void returnsDefaultThresholdsWhenNoneProvided() {
        assertThat(GoalPolicy.assertValidThresholds(null)).isEqualTo(GoalPolicy.DEFAULT_ALERT_THRESHOLDS);
    }

    @Test
    void rejectsAThresholdOutOfRange() {
        assertThatThrownBy(() -> GoalPolicy.assertValidThresholds(List.of(0))).isInstanceOf(InvalidGoalThresholdException.class);
        assertThatThrownBy(() -> GoalPolicy.assertValidThresholds(List.of(101))).isInstanceOf(InvalidGoalThresholdException.class);
    }
}
