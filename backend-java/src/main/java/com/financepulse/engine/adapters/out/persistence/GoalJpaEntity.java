package com.financepulse.engine.adapters.out.persistence;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
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
@Table(name = "goals")
public class GoalJpaEntity {

    @Id
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(nullable = false)
    private String name;

    @Column(name = "target_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal targetAmount;

    @Column(nullable = false)
    private LocalDate deadline;

    @Column(name = "account_id")
    private String accountId;

    @Column(name = "category_id")
    private String categoryId;

    // Eager: mapeado para o domínio fora do escopo de uma sessão Hibernate ativa (mesmo padrão de BudgetJpaEntity.alertThresholds).
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "goal_progress_alert_thresholds", joinColumns = @JoinColumn(name = "goal_id"))
    @Column(name = "threshold")
    private List<Integer> progressAlertThresholds = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected GoalJpaEntity() {
        // exigido pelo JPA/Hibernate
    }

    public GoalJpaEntity(
            String id,
            String userId,
            String name,
            BigDecimal targetAmount,
            LocalDate deadline,
            String accountId,
            String categoryId,
            List<Integer> progressAlertThresholds,
            Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.targetAmount = targetAmount;
        this.deadline = deadline;
        this.accountId = accountId;
        this.categoryId = categoryId;
        this.progressAlertThresholds = new ArrayList<>(progressAlertThresholds);
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getTargetAmount() {
        return targetAmount;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public List<Integer> getProgressAlertThresholds() {
        return progressAlertThresholds;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
