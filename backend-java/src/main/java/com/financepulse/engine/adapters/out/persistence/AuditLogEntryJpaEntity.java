package com.financepulse.engine.adapters.out.persistence;

import com.financepulse.engine.domain.backoffice.AuditAction;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "audit_log_entries")
public class AuditLogEntryJpaEntity {

    @Id
    private String id;

    @Column(name = "operator_user_id", nullable = false)
    private String operatorUserId;

    @Column(name = "target_user_id", nullable = false)
    private String targetUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuditAction action;

    @Column(length = 1000)
    private String details;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AuditLogEntryJpaEntity() {
        // exigido pelo JPA/Hibernate
    }

    public AuditLogEntryJpaEntity(String id, String operatorUserId, String targetUserId, AuditAction action, String details, Instant createdAt) {
        this.id = id;
        this.operatorUserId = operatorUserId;
        this.targetUserId = targetUserId;
        this.action = action;
        this.details = details;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getOperatorUserId() {
        return operatorUserId;
    }

    public String getTargetUserId() {
        return targetUserId;
    }

    public AuditAction getAction() {
        return action;
    }

    public String getDetails() {
        return details;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
