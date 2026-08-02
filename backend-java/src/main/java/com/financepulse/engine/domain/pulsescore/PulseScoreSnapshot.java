package com.financepulse.engine.domain.pulsescore;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

/**
 * RF-034/RF-035 (ver ADR-0020): snapshot diário do Pulse Score, no máximo um
 * por usuário por dia civil ({@code scoreDate}). RN-005: nunca editável pelo
 * usuário — o único caminho de escrita é o próprio cálculo (ver
 * GetDashboardUseCase), nunca um endpoint de escrita direta.
 */
public final class PulseScoreSnapshot {

    private final String id;
    private final String userId;
    private final LocalDate scoreDate;
    private final BigDecimal overallScore;
    private final BigDecimal budgetConsistencyScore;
    private final BigDecimal savingsRateScore;
    private final BigDecimal spendingDiversificationScore;
    private final BigDecimal balanceTrendScore;
    private final String formulaVersion;
    private final Instant createdAt;

    private PulseScoreSnapshot(
            String id,
            String userId,
            LocalDate scoreDate,
            BigDecimal overallScore,
            BigDecimal budgetConsistencyScore,
            BigDecimal savingsRateScore,
            BigDecimal spendingDiversificationScore,
            BigDecimal balanceTrendScore,
            String formulaVersion,
            Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.scoreDate = scoreDate;
        this.overallScore = overallScore;
        this.budgetConsistencyScore = budgetConsistencyScore;
        this.savingsRateScore = savingsRateScore;
        this.spendingDiversificationScore = spendingDiversificationScore;
        this.balanceTrendScore = balanceTrendScore;
        this.formulaVersion = formulaVersion;
        this.createdAt = createdAt;
    }

    public static PulseScoreSnapshot create(
            String id,
            String userId,
            LocalDate scoreDate,
            BigDecimal overallScore,
            BigDecimal budgetConsistencyScore,
            BigDecimal savingsRateScore,
            BigDecimal spendingDiversificationScore,
            BigDecimal balanceTrendScore,
            String formulaVersion) {
        return new PulseScoreSnapshot(
                id,
                userId,
                scoreDate,
                overallScore,
                budgetConsistencyScore,
                savingsRateScore,
                spendingDiversificationScore,
                balanceTrendScore,
                formulaVersion,
                Instant.now());
    }

    public static PulseScoreSnapshot reconstitute(
            String id,
            String userId,
            LocalDate scoreDate,
            BigDecimal overallScore,
            BigDecimal budgetConsistencyScore,
            BigDecimal savingsRateScore,
            BigDecimal spendingDiversificationScore,
            BigDecimal balanceTrendScore,
            String formulaVersion,
            Instant createdAt) {
        return new PulseScoreSnapshot(
                id,
                userId,
                scoreDate,
                overallScore,
                budgetConsistencyScore,
                savingsRateScore,
                spendingDiversificationScore,
                balanceTrendScore,
                formulaVersion,
                createdAt);
    }

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public LocalDate getScoreDate() {
        return scoreDate;
    }

    public BigDecimal getOverallScore() {
        return overallScore;
    }

    public Optional<BigDecimal> getBudgetConsistencyScore() {
        return Optional.ofNullable(budgetConsistencyScore);
    }

    public Optional<BigDecimal> getSavingsRateScore() {
        return Optional.ofNullable(savingsRateScore);
    }

    public Optional<BigDecimal> getSpendingDiversificationScore() {
        return Optional.ofNullable(spendingDiversificationScore);
    }

    public BigDecimal getBalanceTrendScore() {
        return balanceTrendScore;
    }

    public String getFormulaVersion() {
        return formulaVersion;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
