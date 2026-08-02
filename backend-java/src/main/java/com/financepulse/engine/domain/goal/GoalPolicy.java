package com.financepulse.engine.domain.goal;

import com.financepulse.engine.domain.goal.errors.InvalidGoalAssociationException;
import com.financepulse.engine.domain.goal.errors.InvalidGoalDeadlineException;
import com.financepulse.engine.domain.goal.errors.InvalidGoalNameException;
import com.financepulse.engine.domain.goal.errors.InvalidGoalTargetException;
import com.financepulse.engine.domain.goal.errors.InvalidGoalThresholdException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class GoalPolicy {

    public static final int MAX_NAME_LENGTH = 100;
    public static final List<Integer> DEFAULT_ALERT_THRESHOLDS = List.of(80, 100);

    private GoalPolicy() {
    }

    public static String assertValidName(String rawName) {
        String trimmed = rawName == null ? "" : rawName.trim();

        if (trimmed.isEmpty() || trimmed.length() > MAX_NAME_LENGTH) {
            throw new InvalidGoalNameException();
        }

        return trimmed;
    }

    public static void assertPositiveTarget(BigDecimal targetAmount) {
        if (targetAmount == null || targetAmount.signum() <= 0) {
            throw new InvalidGoalTargetException();
        }
    }

    public static void assertFutureDeadline(LocalDate deadline, LocalDate today) {
        if (deadline == null || !deadline.isAfter(today)) {
            throw new InvalidGoalDeadlineException();
        }
    }

    /** ADR-0019: exatamente uma de accountId/categoryId — nunca ambas, nunca nenhuma. */
    public static void assertValidAssociation(String accountId, String categoryId) {
        boolean hasAccount = accountId != null && !accountId.isBlank();
        boolean hasCategory = categoryId != null && !categoryId.isBlank();

        if (hasAccount == hasCategory) {
            throw new InvalidGoalAssociationException();
        }
    }

    /** Duplicado deliberadamente de BudgetPolicy — mesma "regra de três" já aplicada no backend TypeScript (ver ADR-0019). */
    public static List<Integer> assertValidThresholds(List<Integer> thresholds) {
        if (thresholds == null || thresholds.isEmpty()) {
            return DEFAULT_ALERT_THRESHOLDS;
        }
        for (Integer threshold : thresholds) {
            if (threshold == null || threshold < 1 || threshold > 100) {
                throw new InvalidGoalThresholdException();
            }
        }
        return List.copyOf(thresholds);
    }
}
