package com.financepulse.engine.application.services;

import static org.assertj.core.api.Assertions.assertThat;

import com.financepulse.engine.application.services.PulseScoreCalculator.Factor;
import com.financepulse.engine.application.services.PulseScoreCalculator.Input;
import com.financepulse.engine.application.services.PulseScoreCalculator.Result;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PulseScoreCalculatorTest {

    @Test
    void combinesAllFourFactorsWithEqualWeightsWhenAllAreAvailable() {
        Map<String, BigDecimal> expenseByCategory = new LinkedHashMap<>();
        expenseByCategory.put("cat-a", new BigDecimal("300"));
        expenseByCategory.put("cat-b", new BigDecimal("300"));

        Result result = PulseScoreCalculator.calculate(new Input(
                List.of(new BigDecimal("60"), new BigDecimal("120")),
                new BigDecimal("1000"),
                new BigDecimal("600"),
                expenseByCategory,
                new BigDecimal("1100"),
                new BigDecimal("1000")));

        assertThat(result.factors()).hasSize(4);
        assertThat(factorScore(result, "budgetConsistency")).isEqualByComparingTo("90");
        assertThat(factorScore(result, "savingsRate")).isEqualByComparingTo("80");
        assertThat(factorScore(result, "spendingDiversification")).isEqualByComparingTo("50");
        assertThat(factorScore(result, "balanceTrend")).isEqualByComparingTo("55");
        assertThat(result.overallScore()).isEqualByComparingTo("68.75");
    }

    @Test
    void budgetConsistencyGivesFullScoreForBudgetsAtOrUnderTheLimitAndPenalizesOverage() {
        Result result = PulseScoreCalculator.calculate(baseInputWith(
                List.of(new BigDecimal("100"), new BigDecimal("130")), BigDecimal.ZERO, BigDecimal.ZERO, Map.of()));

        assertThat(factorScore(result, "budgetConsistency")).isEqualByComparingTo("85");
    }

    @Test
    void budgetConsistencyFloorsAtZeroForSeverelyOverspentBudgets() {
        Result result = PulseScoreCalculator.calculate(baseInputWith(List.of(new BigDecimal("250")), BigDecimal.ZERO, BigDecimal.ZERO, Map.of()));

        assertThat(factorScore(result, "budgetConsistency")).isEqualByComparingTo("0");
    }

    @Test
    void omitsBudgetConsistencyWhenThereAreNoBudgets() {
        Result result = PulseScoreCalculator.calculate(baseInputWith(List.of(), BigDecimal.ZERO, BigDecimal.ZERO, Map.of()));

        assertThat(factorNames(result)).doesNotContain("budgetConsistency");
    }

    @Test
    void savingsRateSaturatesAtOneHundredForARateOfFiftyPercentOrMore() {
        Result result = PulseScoreCalculator.calculate(baseInputWith(List.of(), new BigDecimal("1000"), new BigDecimal("400"), Map.of()));

        assertThat(factorScore(result, "savingsRate")).isEqualByComparingTo("100");
    }

    @Test
    void savingsRateFloorsAtZeroWhenExpensesExceedIncome() {
        Result result = PulseScoreCalculator.calculate(baseInputWith(List.of(), new BigDecimal("1000"), new BigDecimal("1500"), Map.of()));

        assertThat(factorScore(result, "savingsRate")).isEqualByComparingTo("0");
    }

    @Test
    void omitsSavingsRateWhenThereIsNoIncomeInThePeriod() {
        Result result = PulseScoreCalculator.calculate(baseInputWith(List.of(), BigDecimal.ZERO, new BigDecimal("200"), Map.of()));

        assertThat(factorNames(result)).doesNotContain("savingsRate");
    }

    @Test
    void spendingConcentratedInASingleCategoryScoresZeroDiversification() {
        Result result = PulseScoreCalculator.calculate(
                baseInputWith(List.of(), BigDecimal.ZERO, BigDecimal.ZERO, Map.of("cat-a", new BigDecimal("500"))));

        assertThat(factorScore(result, "spendingDiversification")).isEqualByComparingTo("0");
    }

    @Test
    void omitsSpendingDiversificationWhenThereAreNoExpensesInThePeriod() {
        Result result = PulseScoreCalculator.calculate(baseInputWith(List.of(), BigDecimal.ZERO, BigDecimal.ZERO, Map.of()));

        assertThat(factorNames(result)).doesNotContain("spendingDiversification");
    }

    @Test
    void balanceTrendIsAlwaysPresentEvenWhenEveryOtherFactorIsOmitted() {
        Result result = PulseScoreCalculator.calculate(baseInputWith(List.of(), BigDecimal.ZERO, BigDecimal.ZERO, Map.of()));

        assertThat(result.factors()).hasSize(1);
        assertThat(factorNames(result)).containsExactly("balanceTrend");
        assertThat(result.overallScore()).isEqualByComparingTo(factorScore(result, "balanceTrend"));
    }

    @Test
    void balanceTrendScoresFiftyWhenThePastBalanceWasZeroAndNothingChanged() {
        Result result = PulseScoreCalculator.calculate(new Input(List.of(), BigDecimal.ZERO, BigDecimal.ZERO, Map.of(), BigDecimal.ZERO, BigDecimal.ZERO));

        assertThat(factorScore(result, "balanceTrend")).isEqualByComparingTo("50");
    }

    @Test
    void balanceTrendScoresOneHundredWhenThePastBalanceWasZeroAndTheBalanceGrew() {
        Result result = PulseScoreCalculator.calculate(
                new Input(List.of(), BigDecimal.ZERO, BigDecimal.ZERO, Map.of(), new BigDecimal("50"), BigDecimal.ZERO));

        assertThat(factorScore(result, "balanceTrend")).isEqualByComparingTo("100");
    }

    @Test
    void balanceTrendScoresZeroWhenThePastBalanceWasZeroAndTheBalanceFell() {
        Result result = PulseScoreCalculator.calculate(
                new Input(List.of(), BigDecimal.ZERO, BigDecimal.ZERO, Map.of(), new BigDecimal("-50"), BigDecimal.ZERO));

        assertThat(factorScore(result, "balanceTrend")).isEqualByComparingTo("0");
    }

    private static Input baseInputWith(
            List<BigDecimal> budgetConsumptionPercentages, BigDecimal totalIncome, BigDecimal totalExpense, Map<String, BigDecimal> expenseByCategory) {
        return new Input(budgetConsumptionPercentages, totalIncome, totalExpense, expenseByCategory, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    private static BigDecimal factorScore(Result result, String name) {
        return result.factors().stream().filter(factor -> factor.name().equals(name)).map(Factor::score).findFirst().orElseThrow();
    }

    private static List<String> factorNames(Result result) {
        return result.factors().stream().map(Factor::name).toList();
    }
}
