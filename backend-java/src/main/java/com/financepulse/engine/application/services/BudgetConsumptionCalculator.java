package com.financepulse.engine.application.services;

import com.financepulse.engine.application.services.BudgetPeriodCalculator.PeriodRange;
import com.financepulse.engine.domain.budget.Budget;
import com.financepulse.engine.domain.transaction.Transaction;
import com.financepulse.engine.domain.transaction.TransactionType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * RF-027: percentual de consumo em tempo real. RF-028 (sinal, entrega adiada
 * — ver ADR-0018): calcula quais limiares configurados já foram
 * ultrapassados. Apenas transações de despesa (EXPENSE) consomem o
 * orçamento — RF-026 fala em "limite de gasto".
 */
public final class BudgetConsumptionCalculator {

    private BudgetConsumptionCalculator() {
    }

    public static Consumption calculate(Budget budget, PeriodRange period, List<Transaction> categoryTransactions) {
        BigDecimal consumedAmount = categoryTransactions.stream()
                .filter(transaction -> transaction.getType() == TransactionType.EXPENSE)
                .filter(transaction -> period.contains(transaction.getDate()))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal percentage = consumedAmount
                .divide(budget.getLimitAmount(), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        List<Integer> thresholdsCrossed = budget.getAlertThresholds().stream()
                .filter(threshold -> percentage.compareTo(BigDecimal.valueOf(threshold)) >= 0)
                .sorted()
                .toList();

        return new Consumption(period, consumedAmount, percentage, thresholdsCrossed);
    }

    public record Consumption(PeriodRange period, BigDecimal consumedAmount, BigDecimal percentage, List<Integer> thresholdsCrossed) {
    }
}
