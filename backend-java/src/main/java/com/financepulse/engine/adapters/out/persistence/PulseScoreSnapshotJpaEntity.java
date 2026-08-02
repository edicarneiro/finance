package com.financepulse.engine.adapters.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "pulse_score_snapshots", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "score_date"}))
public class PulseScoreSnapshotJpaEntity {

    @Id
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "score_date", nullable = false)
    private LocalDate scoreDate;

    @Column(name = "overall_score", nullable = false, precision = 19, scale = 4)
    private BigDecimal overallScore;

    @Column(name = "budget_consistency_score", precision = 19, scale = 4)
    private BigDecimal budgetConsistencyScore;

    @Column(name = "savings_rate_score", precision = 19, scale = 4)
    private BigDecimal savingsRateScore;

    @Column(name = "spending_diversification_score", precision = 19, scale = 4)
    private BigDecimal spendingDiversificationScore;

    @Column(name = "balance_trend_score", nullable = false, precision = 19, scale = 4)
    private BigDecimal balanceTrendScore;

    @Column(name = "formula_version", nullable = false)
    private String formulaVersion;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PulseScoreSnapshotJpaEntity() {
        // exigido pelo JPA/Hibernate
    }

    public PulseScoreSnapshotJpaEntity(
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

    public BigDecimal getBudgetConsistencyScore() {
        return budgetConsistencyScore;
    }

    public BigDecimal getSavingsRateScore() {
        return savingsRateScore;
    }

    public BigDecimal getSpendingDiversificationScore() {
        return spendingDiversificationScore;
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
