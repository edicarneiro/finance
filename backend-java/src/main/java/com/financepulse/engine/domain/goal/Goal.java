package com.financepulse.engine.domain.goal;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

/** RF-030/RF-031. {@code accountId}/{@code categoryId} são mutuamente exclusivos e imutáveis após a criação (ver ADR-0019). */
public final class Goal {

    private final String id;
    private final String userId;
    private final String name;
    private final BigDecimal targetAmount;
    private final LocalDate deadline;
    private final String accountId;
    private final String categoryId;
    private final List<Integer> progressAlertThresholds;
    private final Instant createdAt;

    private Goal(
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
        this.progressAlertThresholds = List.copyOf(progressAlertThresholds);
        this.createdAt = createdAt;
    }

    public static Goal create(
            String id,
            String userId,
            String name,
            BigDecimal targetAmount,
            LocalDate deadline,
            String accountId,
            String categoryId,
            List<Integer> progressAlertThresholds,
            LocalDate today) {
        String validatedName = GoalPolicy.assertValidName(name);
        GoalPolicy.assertPositiveTarget(targetAmount);
        GoalPolicy.assertFutureDeadline(deadline, today);
        GoalPolicy.assertValidAssociation(accountId, categoryId);
        List<Integer> validatedThresholds = GoalPolicy.assertValidThresholds(progressAlertThresholds);

        // createdAt deriva de `today` (a mesma referência de Clock usada para validar o prazo), não de Instant.now():
        // ListGoalsUseCase usa esta data para delimitar "transações desde a criação da meta" (RF-031/ADR-0019) — um
        // relógio de parede não controlado por FixedClock nos testes quebraria essa regra de negócio (ver ADR-0019 § addendum).
        return new Goal(
                id, userId, validatedName, targetAmount, deadline, accountId, categoryId, validatedThresholds, today.atStartOfDay(ZoneOffset.UTC).toInstant());
    }

    public static Goal reconstitute(
            String id,
            String userId,
            String name,
            BigDecimal targetAmount,
            LocalDate deadline,
            String accountId,
            String categoryId,
            List<Integer> progressAlertThresholds,
            Instant createdAt) {
        return new Goal(id, userId, name, targetAmount, deadline, accountId, categoryId, progressAlertThresholds, createdAt);
    }

    /** RF-030: nome, valor-alvo, prazo e limiares são editáveis — a associação de conta/categoria é imutável (ver ADR-0019). */
    public Goal withDetails(String newName, BigDecimal newTargetAmount, LocalDate newDeadline, List<Integer> newThresholds, LocalDate today) {
        String validatedName = GoalPolicy.assertValidName(newName);
        GoalPolicy.assertPositiveTarget(newTargetAmount);
        GoalPolicy.assertFutureDeadline(newDeadline, today);
        List<Integer> validatedThresholds = GoalPolicy.assertValidThresholds(newThresholds);

        return new Goal(id, userId, validatedName, newTargetAmount, newDeadline, accountId, categoryId, validatedThresholds, createdAt);
    }

    public boolean isAccountBased() {
        return accountId != null;
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

    public Optional<String> getAccountId() {
        return Optional.ofNullable(accountId);
    }

    public Optional<String> getCategoryId() {
        return Optional.ofNullable(categoryId);
    }

    public List<Integer> getProgressAlertThresholds() {
        return progressAlertThresholds;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
