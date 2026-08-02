package com.financepulse.engine.domain.transaction;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class Transaction {

    private final String id;
    private final String userId;
    private final String accountId;
    private final String categoryId;
    private final TransactionType type;
    private final BigDecimal amount;
    private final LocalDate date;
    private final String description;
    private final List<String> tags;
    private final Instant createdAt;

    private Transaction(
            String id,
            String userId,
            String accountId,
            String categoryId,
            TransactionType type,
            BigDecimal amount,
            LocalDate date,
            String description,
            List<String> tags,
            Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.accountId = accountId;
        this.categoryId = categoryId;
        this.type = type;
        this.amount = amount;
        this.date = date;
        this.description = description;
        this.tags = List.copyOf(tags == null ? List.of() : tags);
        this.createdAt = createdAt;
    }

    public static Transaction create(
            String id,
            String userId,
            String accountId,
            String categoryId,
            TransactionType type,
            BigDecimal amount,
            LocalDate date,
            String description,
            List<String> tags) {
        TransactionPolicy.assertPositiveAmount(amount);
        return new Transaction(id, userId, accountId, categoryId, type, amount, date, description, tags, Instant.now());
    }

    public static Transaction reconstitute(
            String id,
            String userId,
            String accountId,
            String categoryId,
            TransactionType type,
            BigDecimal amount,
            LocalDate date,
            String description,
            List<String> tags,
            Instant createdAt) {
        return new Transaction(id, userId, accountId, categoryId, type, amount, date, description, tags, createdAt);
    }

    /** RF-015: edição — todos os campos (exceto id/userId/createdAt) podem ser substituídos. */
    public Transaction withDetails(
            String accountId,
            String categoryId,
            TransactionType type,
            BigDecimal amount,
            LocalDate date,
            String description,
            List<String> tags) {
        TransactionPolicy.assertPositiveAmount(amount);
        return new Transaction(id, userId, accountId, categoryId, type, amount, date, description, tags, createdAt);
    }

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public TransactionType getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDate getDate() {
        return date;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getTags() {
        return tags;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
