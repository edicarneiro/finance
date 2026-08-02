package com.financepulse.engine.application.services;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;

/**
 * RF-042: sinaliza uma transação como atípica quando seu valor excede a
 * média histórica da categoria em mais de {@link #SIGMA_MULTIPLIER} desvios-
 * padrão — fórmula provisória e transparente (RF-042 não especifica o
 * método estatístico exato, mesmo espírito de {@code PulseScoreCalculator},
 * ver ADR-0022/0020). Função pura, sem I/O. Exige uma amostra histórica
 * mínima ({@link #MIN_SAMPLE_SIZE}) antes de avaliar qualquer coisa — dado
 * insuficiente nunca é tratado como atípico por padrão.
 */
public final class AtypicalSpendingDetector {

    public static final int MIN_SAMPLE_SIZE = 5;
    public static final BigDecimal SIGMA_MULTIPLIER = BigDecimal.valueOf(2);

    private static final MathContext MATH_CONTEXT = new MathContext(10);

    private AtypicalSpendingDetector() {
    }

    public static boolean isAtypical(List<BigDecimal> historicalAmounts, BigDecimal candidateAmount) {
        if (historicalAmounts.size() < MIN_SAMPLE_SIZE) {
            return false;
        }

        BigDecimal mean = mean(historicalAmounts);
        BigDecimal standardDeviation = standardDeviation(historicalAmounts, mean);
        BigDecimal threshold = mean.add(standardDeviation.multiply(SIGMA_MULTIPLIER));

        return candidateAmount.compareTo(threshold) > 0;
    }

    private static BigDecimal mean(List<BigDecimal> amounts) {
        BigDecimal sum = amounts.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(amounts.size()), MATH_CONTEXT);
    }

    private static BigDecimal standardDeviation(List<BigDecimal> amounts, BigDecimal mean) {
        BigDecimal sumOfSquaredDeviations = amounts.stream()
                .map(amount -> amount.subtract(mean).pow(2))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal variance = sumOfSquaredDeviations.divide(BigDecimal.valueOf(amounts.size()), MATH_CONTEXT);

        return variance.sqrt(MATH_CONTEXT).setScale(4, RoundingMode.HALF_UP);
    }
}
