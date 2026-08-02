package com.financepulse.engine.adapters.out.persistence;

import com.financepulse.engine.domain.user.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "users")
public class UserJpaEntity {

    @Id
    private String id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    private String name;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(name = "suspended_at")
    private Instant suspendedAt;

    protected UserJpaEntity() {
        // exigido pelo JPA/Hibernate
    }

    public UserJpaEntity(
            String id, String email, String passwordHash, String name, Instant createdAt, Instant deletedAt, Role role, Instant suspendedAt) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.name = name;
        this.createdAt = createdAt;
        this.deletedAt = deletedAt;
        this.role = role;
        this.suspendedAt = suspendedAt;
    }

    public String getId() {
        return id;
    }

    public String getEmail() {
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

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public Role getRole() {
        return role;
    }

    public Instant getSuspendedAt() {
        return suspendedAt;
    }
}
