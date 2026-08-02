package com.financepulse.engine.domain.budget;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** RF-026/RN-004. {@code categoryId} e {@code periodType} são imutáveis após a criação (ver ADR-0018). */
public final class Budget {

    private final String id;
    private final String userId;
    private final String categoryId;
    private final BigDecimal limitAmount;
    private final BudgetPeriodType periodType;
    private final LocalDate customPeriodStart;
    private final LocalDate customPeriodEnd;
    private final List<Integer> alertThresholds;
    private final Instant createdAt;

    private Budget(
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
        this.alertThresholds = List.copyOf(alertThresholds);
        this.createdAt = createdAt;
    }

    public static Budget create(
            String id,
            String userId,
            String categoryId,
            BigDecimal limitAmount,
            BudgetPeriodType periodType,
            LocalDate customPeriodStart,
            LocalDate customPeriodEnd,
            List<Integer> alertThresholds) {
        BudgetPolicy.assertPositiveLimit(limitAmount);
        BudgetPolicy.assertValidPeriod(periodType, customPeriodStart, customPeriodEnd);
        List<Integer> validatedThresholds = BudgetPolicy.assertValidThresholds(alertThresholds);

        return new Budget(
                id, userId, categoryId, limitAmount, periodType, customPeriodStart, customPeriodEnd, validatedThresholds, Instant.now());
    }

    public static Budget reconstitute(
            String id,
            String userId,
            String categoryId,
            BigDecimal limitAmount,
            BudgetPeriodType periodType,
            LocalDate customPeriodStart,
            LocalDate customPeriodEnd,
            List<Integer> alertThresholds,
            Instant createdAt) {
        return new Budget(
                id, userId, categoryId, limitAmount, periodType, customPeriodStart, customPeriodEnd, alertThresholds, createdAt);
    }

    /** RF-026: apenas limite e limiares de alerta são editáveis — categoria e tipo de período são imutáveis (ver ADR-0018). */
    public Budget withLimitAndThresholds(BigDecimal newLimitAmount, List<Integer> newAlertThresholds) {
        BudgetPolicy.assertPositiveLimit(newLimitAmount);
        List<Integer> validatedThresholds = BudgetPolicy.assertValidThresholds(newAlertThresholds);

        return new Budget(
                id, userId, categoryId, newLimitAmount, periodType, customPeriodStart, customPeriodEnd, validatedThresholds, createdAt);
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

    public Optional<LocalDate> getCustomPeriodStart() {
        return Optional.ofNullable(customPeriodStart);
    }

    public Optional<LocalDate> getCustomPeriodEnd() {
        return Optional.ofNullable(customPeriodEnd);
    }

    public List<Integer> getAlertThresholds() {
        return alertThresholds;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
