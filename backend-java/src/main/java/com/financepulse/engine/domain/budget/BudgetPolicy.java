package com.financepulse.engine.domain.budget;

import com.financepulse.engine.domain.budget.errors.InvalidAlertThresholdException;
import com.financepulse.engine.domain.budget.errors.InvalidBudgetLimitException;
import com.financepulse.engine.domain.budget.errors.InvalidBudgetPeriodException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class BudgetPolicy {

    public static final List<Integer> DEFAULT_ALERT_THRESHOLDS = List.of(80, 100);

    private BudgetPolicy() {
    }

    public static void assertPositiveLimit(BigDecimal limitAmount) {
        if (limitAmount == null || limitAmount.signum() <= 0) {
            throw new InvalidBudgetLimitException();
        }
    }

    public static List<Integer> assertValidThresholds(List<Integer> thresholds) {
        if (thresholds == null || thresholds.isEmpty()) {
            return DEFAULT_ALERT_THRESHOLDS;
        }
        for (Integer threshold : thresholds) {
            if (threshold == null || threshold < 1 || threshold > 100) {
                throw new InvalidAlertThresholdException();
            }
        }
        return List.copyOf(thresholds);
    }

    /** ADR-0018: CUSTOM exige um intervalo válido; MONTHLY/WEEKLY não aceitam datas customizadas. */
    public static void assertValidPeriod(BudgetPeriodType periodType, LocalDate customPeriodStart, LocalDate customPeriodEnd) {
        if (periodType == BudgetPeriodType.CUSTOM) {
            if (customPeriodStart == null || customPeriodEnd == null) {
                throw new InvalidBudgetPeriodException("Orçamentos de período customizado exigem data de início e de fim.");
            }
            if (!customPeriodStart.isBefore(customPeriodEnd)) {
                throw new InvalidBudgetPeriodException("A data de início deve ser anterior à data de fim.");
            }
        } else if (customPeriodStart != null || customPeriodEnd != null) {
            throw new InvalidBudgetPeriodException("Datas customizadas só se aplicam a orçamentos de período CUSTOM.");
        }
    }
}
