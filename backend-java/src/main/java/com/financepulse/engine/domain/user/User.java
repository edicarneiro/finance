package com.financepulse.engine.domain.user;

import java.time.Instant;

public final class User {

    private final String id;
    private final Email email;
    private final String passwordHash;
    private final String name;
    private final Instant createdAt;

    private User(String id, Email email, String passwordHash, String name, Instant createdAt) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.name = name;
        this.createdAt = createdAt;
    }

    public static User register(String id, Email email, String passwordHash) {
        return new User(id, email, passwordHash, null, Instant.now());
    }

    public static User reconstitute(String id, Email email, String passwordHash, String name, Instant createdAt) {
        return new User(id, email, passwordHash, name, createdAt);
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
}
