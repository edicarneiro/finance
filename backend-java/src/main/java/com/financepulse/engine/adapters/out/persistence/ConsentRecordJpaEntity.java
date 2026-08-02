package com.financepulse.engine.adapters.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "consent_records")
public class ConsentRecordJpaEntity {

    @Id
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(nullable = false)
    private String version;

    @Column(name = "accepted_at", nullable = false)
    private Instant acceptedAt;

    protected ConsentRecordJpaEntity() {
        // exigido pelo JPA/Hibernate
    }

    public ConsentRecordJpaEntity(String id, String userId, String version, Instant acceptedAt) {
        this.id = id;
        this.userId = userId;
        this.version = version;
        this.acceptedAt = acceptedAt;
    }

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getVersion() {
        return version;
    }

    public Instant getAcceptedAt() {
        return acceptedAt;
    }
}
