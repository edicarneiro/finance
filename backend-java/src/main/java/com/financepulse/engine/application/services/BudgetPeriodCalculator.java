package com.financepulse.engine.application.services;

import com.financepulse.engine.domain.budget.Budget;
import com.financepulse.engine.domain.budget.BudgetPeriodType;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

/**
 * RN-004: deriva o intervalo do período vigente (ou de períodos anteriores,
 * para RF-029) de um orçamento. MONTHLY/WEEKLY são recorrentes — calculados a
 * partir da data de referência, nunca armazenados; CUSTOM é um intervalo fixo
 * sem períodos anteriores (ver ADR-0018).
 */
public final class BudgetPeriodCalculator {

    private BudgetPeriodCalculator() {
    }

    public static PeriodRange currentPeriod(Budget budget, LocalDate today) {
        return periodContaining(budget, today);
    }

    /** Períodos anteriores ao vigente, mais recente primeiro. CUSTOM não tem períodos anteriores (lista vazia). */
    public static List<PeriodRange> previousPeriods(Budget budget, LocalDate today, int count) {
        if (budget.getPeriodType() == BudgetPeriodType.CUSTOM) {
            return List.of();
        }

        List<PeriodRange> periods = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            LocalDate reference = budget.getPeriodType() == BudgetPeriodType.MONTHLY ? today.minusMonths(i) : today.minusWeeks(i);
            periods.add(periodContaining(budget, reference));
        }
        return List.copyOf(periods);
    }

    private static PeriodRange periodContaining(Budget budget, LocalDate reference) {
        return switch (budget.getPeriodType()) {
            case MONTHLY -> new PeriodRange(reference.withDayOfMonth(1), reference.with(TemporalAdjusters.lastDayOfMonth()));
            case WEEKLY -> new PeriodRange(
                    reference.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
                    reference.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)));
            case CUSTOM -> new PeriodRange(
                    budget.getCustomPeriodStart().orElseThrow(), budget.getCustomPeriodEnd().orElseThrow());
        };
    }

    public record PeriodRange(LocalDate start, LocalDate end) {

        public boolean contains(LocalDate date) {
            return !date.isBefore(start) && !date.isAfter(end);
        }
    }
}
