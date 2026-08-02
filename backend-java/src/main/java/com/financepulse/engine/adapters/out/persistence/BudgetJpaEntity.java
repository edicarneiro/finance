package com.financepulse.engine.adapters.out.persistence;

import com.financepulse.engine.domain.budget.BudgetPeriodType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "budgets")
public class BudgetJpaEntity {

    @Id
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "category_id", nullable = false)
    private String categoryId;

    @Column(name = "limit_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal limitAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "period_type", nullable = false)
    private BudgetPeriodType periodType;

    @Column(name = "custom_period_start")
    private LocalDate customPeriodStart;

    @Column(name = "custom_period_end")
    private LocalDate customPeriodEnd;

    // Eager: mapeado para o domínio fora do escopo de uma sessão Hibernate ativa (mesmo padrão de TransactionJpaEntity.tags).
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "budget_alert_thresholds", joinColumns = @JoinColumn(name = "budget_id"))
    @Column(name = "threshold")
    private List<Integer> alertThresholds = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected BudgetJpaEntity() {
        // exigido pelo JPA/Hibernate
    }

    public BudgetJpaEntity(
            String id,
            String userId,
            String categoryId,
            BigDecimal limitAmount,
            BudgetPeriodType periodType,
            LocalDate customPeriodStart,
            LocalDate customPeriodEnd,
            List<Integer> alertThresholds,
            Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.categoryId = categoryId;
        this.limitAmount = limitAmount;
        this.periodType = periodType;
        this.customPeriodStart = customPeriodStart;
        this.customPeriodEnd = customPeriodEnd;
        this.alertThresholds = new ArrayList<>(alertThresholds);
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public BigDecimal getLimitAmount() {
        return limitAmount;
    }

    public BudgetPeriodType getPeriodType() {
        return periodType;
    }

    public LocalDate getCustomPeriodStart() {
        return customPeriodStart;
    }

    public LocalDate getCustomPeriodEnd() {
        return customPeriodEnd;
    }

    public List<Integer> getAlertThresholds() {
        return alertThresholds;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
