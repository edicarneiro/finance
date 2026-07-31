package com.financepulse.engine.adapters.out.persistence;

import com.financepulse.engine.domain.account.AccountType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "accounts")
public class AccountJpaEntity {

    @Id
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountType type;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String currency;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balance;

    @Column(nullable = false)
    private boolean archived;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AccountJpaEntity() {
        // exigido pelo JPA/Hibernate
    }

    public AccountJpaEntity(
            String id,
            String userId,
            AccountType type,
            String name,
            String currency,
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

    public String getCurrency() {
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
