package com.financepulse.engine.application.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * RF-034/RF-036 (ver ADR-0020): fórmula provisória e transparente do Pulse
 * Score, composta pelos quatro sinais citados em vision.md § 4.8
 * (consistência orçamentária, taxa de poupança, diversificação de gastos,
 * tendência de saldo), com pesos iguais. RN-006 declara formalmente que a
 * composição exata é uma decisão de produto/ciência de dados ainda pendente
 * — {@link #FORMULA_VERSION} identifica esta implementação como provisória,
 * não como a definição final. Função pura, sem I/O.
 */
public final class PulseScoreCalculator {

    public static final String FORMULA_VERSION = "pulse-v0-provisional";

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal FIFTY = BigDecimal.valueOf(50);
    private static final BigDecimal HALF = new BigDecimal("0.5");
    private static final BigDecimal EQUAL_WEIGHT = BigDecimal.ONE;

    private PulseScoreCalculator() {
    }

    public static Result calculate(Input input) {
        List<Factor> factors = new ArrayList<>();

        budgetConsistency(input.budgetConsumptionPercentages()).ifPresent(factors::add);
        savingsRate(input.totalIncome(), input.totalExpense()).ifPresent(factors::add);
        spendingDiversification(input.expenseByCategory()).ifPresent(factors::add);
        factors.add(balanceTrend(input.balanceNow(), input.balancePast()));

        BigDecimal weightSum = factors.stream().map(Factor::weight).reduce(ZERO, BigDecimal::add);
        BigDecimal weightedSum = factors.stream().map(factor -> factor.score().multiply(factor.weight())).reduce(ZERO, BigDecimal::add);
        BigDecimal overallScore = weightedSum.divide(weightSum, 4, RoundingMode.HALF_UP);

        return new Result(overallScore, List.copyOf(factors));
    }

    private static Optional<Factor> budgetConsistency(List<BigDecimal> consumptionPercentages) {
        if (consumptionPercentages.isEmpty()) {
            return Optional.empty();
        }

        BigDecimal sum = consumptionPercentages.stream().map(PulseScoreCalculator::budgetHealthScore).reduce(ZERO, BigDecimal::add);
        BigDecimal average = sum.divide(BigDecimal.valueOf(consumptionPercentages.size()), 4, RoundingMode.HALF_UP);

        return Optional.of(new Factor("budgetConsistency", average, EQUAL_WEIGHT));
    }

    private static BigDecimal budgetHealthScore(BigDecimal consumedPercentage) {
        if (consumedPercentage.compareTo(HUNDRED) <= 0) {
            return HUNDRED;
        }
        return clamp(HUNDRED.subtract(consumedPercentage.subtract(HUNDRED)));
    }

    private static Optional<Factor> savingsRate(BigDecimal totalIncome, BigDecimal totalExpense) {
        if (totalIncome.signum() <= 0) {
            return Optional.empty();
        }

        BigDecimal rate = totalIncome.subtract(totalExpense).divide(totalIncome, 6, RoundingMode.HALF_UP);
        BigDecimal score = clamp(rate.divide(HALF, 6, RoundingMode.HALF_UP).multiply(HUNDRED));

        return Optional.of(new Factor("savingsRate", score, EQUAL_WEIGHT));
    }

    private static Optional<Factor> spendingDiversification(Map<String, BigDecimal> expenseByCategory) {
        BigDecimal total = expenseByCategory.values().stream().reduce(ZERO, BigDecimal::add);
        if (total.signum() <= 0) {
            return Optional.empty();
        }

        BigDecimal herfindahlIndex = expenseByCategory.values().stream()
                .map(amount -> amount.divide(total, 6, RoundingMode.HALF_UP))
                .map(share -> share.multiply(share))
                .reduce(ZERO, BigDecimal::add);
        BigDecimal score = clamp(BigDecimal.ONE.subtract(herfindahlIndex).multiply(HUNDRED));

        return Optional.of(new Factor("spendingDiversification", score, EQUAL_WEIGHT));
    }

    private static Factor balanceTrend(BigDecimal balanceNow, BigDecimal balancePast) {
        BigDecimal delta = balanceNow.subtract(balancePast);
        BigDecimal reference = balancePast.abs();

        BigDecimal score;
        if (reference.signum() == 0) {
            score = delta.signum() > 0 ? HUNDRED : delta.signum() < 0 ? ZERO : FIFTY;
        } else {
            BigDecimal ratio = delta.divide(reference, 6, RoundingMode.HALF_UP);
            score = clamp(FIFTY.add(ratio.multiply(FIFTY)));
        }

        return new Factor("balanceTrend", score, EQUAL_WEIGHT);
    }

    private static BigDecimal clamp(BigDecimal value) {
        if (value.compareTo(ZERO) < 0) {
            return ZERO;
        }
        if (value.compareTo(HUNDRED) > 0) {
            return HUNDRED;
        }
        return value;
    }

    public record Input(
            List<BigDecimal> budgetConsumptionPercentages,
            BigDecimal totalIncome,
            BigDecimal totalExpense,
            Map<String, BigDecimal> expenseByCategory,
            BigDecimal balanceNow,
            BigDecimal balancePast) {
    }

    public record Factor(String name, BigDecimal score, BigDecimal weight) {
    }

    public record Result(BigDecimal overallScore, List<Factor> factors) {
    }
}
