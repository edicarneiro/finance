package com.financepulse.engine.domain.user;

import java.time.Instant;
import java.util.Optional;

public final class User {

    private final String id;
    private final Email email;
    private final String passwordHash;
    private final String name;
    private final Instant createdAt;
    private final Instant deletedAt;
    private final Role role;
    private final Instant suspendedAt;

    private User(
            String id, Email email, String passwordHash, String name, Instant createdAt, Instant deletedAt, Role role, Instant suspendedAt) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.name = name;
        this.createdAt = createdAt;
        this.deletedAt = deletedAt;
        this.role = role;
        this.suspendedAt = suspendedAt;
    }

    public static User register(String id, Email email, String passwordHash) {
        return new User(id, email, passwordHash, null, Instant.now(), null, Role.CUSTOMER, null);
    }

    public static User reconstitute(
            String id, Email email, String passwordHash, String name, Instant createdAt, Instant deletedAt, Role role, Instant suspendedAt) {
        return new User(id, email, passwordHash, name, createdAt, deletedAt, role, suspendedAt);
    }

    /**
     * RF-045/RF-007 (ver ADR-0023): anonimização, não exclusão física — mesma
     * decisão do backend TypeScript (ADR-0010). A linha permanece no banco
     * (id preservado); nenhum dado pessoal identificável resta associado a
     * ela. {@code role}/{@code suspendedAt} são preservados — a anonimização
     * não é uma ação de moderação.
     */
    public User anonymize(Email anonymizedEmail, String unusablePasswordHash, Instant deletedAt) {
        return new User(id, anonymizedEmail, unusablePasswordHash, null, createdAt, deletedAt, role, suspendedAt);
    }

    /** RF-050 (ver ADR-0024): bloqueio de acesso reversível, distinto da anonimização — não altera email/nome/senha. */
    public User suspend(Instant suspendedAt) {
        return new User(id, email, passwordHash, name, createdAt, deletedAt, role, suspendedAt);
    }

    public User reactivate() {
        return new User(id, email, passwordHash, name, createdAt, deletedAt, role, null);
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public boolean isSuspended() {
        return suspendedAt != null;
    }

    public boolean isSupportOperator() {
        return role == Role.SUPPORT_OPERATOR;
    }

    public String getId() {
        return id;
    }

    public Email getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getName() {
        return name;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Optional<Instant> getDeletedAt() {
        return Optional.ofNullable(deletedAt);
    }

    public Role getRole() {
        return role;
    }

    public Optional<Instant> getSuspendedAt() {
        return Optional.ofNullable(suspendedAt);
    }
}
