package com.financepulse.engine.application.services;

import com.financepulse.engine.domain.goal.Goal;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

/**
 * RF-031: percentual de progresso. RF-032 (sinal, entrega adiada — ver
 * ADR-0019): calcula limiares ultrapassados e se a meta foi atingida. Função
 * pura — quem calcula {@code currentAmount} (saldo de conta ou soma líquida
 * de categoria) é o caso de uso, que tem acesso aos repositórios.
 */
public final class GoalProgressCalculator {

    private GoalProgressCalculator() {
    }

    public static Progress calculate(Goal goal, BigDecimal currentAmount, LocalDate today) {
        BigDecimal percentage = currentAmount
                .divide(goal.getTargetAmount(), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        List<Integer> thresholdsCrossed = goal.getProgressAlertThresholds().stream()
                .filter(threshold -> percentage.compareTo(BigDecimal.valueOf(threshold)) >= 0)
                .sorted()
                .toList();

        boolean achieved = currentAmount.compareTo(goal.getTargetAmount()) >= 0;
        boolean overdue = !achieved && today.isAfter(goal.getDeadline());

        return new Progress(currentAmount, percentage, thresholdsCrossed, achieved, overdue);
    }

    public record Progress(BigDecimal currentAmount, BigDecimal percentage, List<Integer> thresholdsCrossed, boolean achieved, boolean overdue) {
    }
}
