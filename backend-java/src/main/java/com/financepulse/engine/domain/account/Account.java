package com.financepulse.engine.domain.account;

import java.math.BigDecimal;
import java.time.Instant;

public final class Account {

    private final String id;
    private final String userId;
    private final AccountType type;
    private final String name;
    private final Currency currency;
    private final BigDecimal balance;
    private final boolean archived;
    private final Instant createdAt;

    private Account(
            String id,
            String userId,
            AccountType type,
            String name,
            Currency currency,
            BigDecimal balance,
            boolean archived,
            Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.type = type;
        this.name = name;
        this.currency = currency;
        this.balance = balance;
        this.archived = archived;
        this.createdAt = createdAt;
    }

    /**
     * RN-001: o saldo só é gravável na criação (saldo inicial). A partir daqui,
     * o saldo é sempre derivado da soma das transações da conta — nesta fase
     * (Fase 3), sem transações ainda existirem (Fase 4), o saldo atual é
     * necessariamente igual ao saldo inicial (ver ADR-0014).
     */
    public static Account create(
            String id, String userId, AccountType type, String name, Currency currency, BigDecimal initialBalance) {
        String validatedName = AccountPolicy.assertValidName(name);
        return new Account(id, userId, type, validatedName, currency, initialBalance, false, Instant.now());
    }

    public static Account reconstitute(
            String id,
            String userId,
            AccountType type,
            String name,
            Currency currency,
            BigDecimal balance,
            boolean archived,
            Instant createdAt) {
        return new Account(id, userId, type, name, currency, balance, archived, createdAt);
    }

    /** RF-010: apenas o nome é editável — tipo e moeda são imutáveis após a criação (ver ADR-0014). */
    public Account withName(String newName) {
        String validatedName = AccountPolicy.assertValidName(newName);
        return new Account(id, userId, type, validatedName, currency, balance, archived, createdAt);
    }

    /** RF-010/RF-013: arquivamento é idempotente — arquivar uma conta já arquivada não é erro. */
    public Account archive() {
        if (archived) {
            return this;
        }
        return new Account(id, userId, type, name, currency, balance, true, createdAt);
    }

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public AccountType getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public Currency getCurrency() {
        return currency;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public boolean isArchived() {
        return archived;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
