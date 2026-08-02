package com.financepulse.engine.application.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class AtypicalSpendingDetectorTest {

    @Test
    void neverFlagsAnythingWhenTheHistoricalSampleIsBelowTheMinimumSize() {
        List<BigDecimal> historical = amounts("10", "10", "10", "10");

        assertThat(AtypicalSpendingDetector.isAtypical(historical, new BigDecimal("10000"))).isFalse();
    }

    @Test
    void flagsAValueAboveMeanPlusTwoStandardDeviationsAsAtypical() {
        List<BigDecimal> historical = amounts("10", "10", "10", "10", "10");

        assertThat(AtypicalSpendingDetector.isAtypical(historical, new BigDecimal("11"))).isTrue();
    }

    @Test
    void doesNotFlagAValueEqualToTheHistoricalMeanWhenThereIsNoVariance() {
        List<BigDecimal> historical = amounts("10", "10", "10", "10", "10");

        assertThat(AtypicalSpendingDetector.isAtypical(historical, new BigDecimal("10"))).isFalse();
    }

    @Test
    void toleratesNaturalVarianceWithinTwoStandardDeviations() {
        List<BigDecimal> historical = amounts("10", "20", "30", "40", "50");

        assertThat(AtypicalSpendingDetector.isAtypical(historical, new BigDecimal("50"))).isFalse();
        assertThat(AtypicalSpendingDetector.isAtypical(historical, new BigDecimal("500"))).isTrue();
    }

    private static List<BigDecimal> amounts(String... values) {
        return Stream.of(values).map(BigDecimal::new).toList();
    }
}
