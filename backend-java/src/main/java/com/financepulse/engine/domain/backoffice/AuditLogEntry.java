package com.financepulse.engine.domain.backoffice;

import java.time.Instant;

/**
 * RF-048 (ver ADR-0024): trilha append-only de todo acesso administrativo/
 * backoffice a dados de um usuário — nunca atualizada ou apagada. Consultar
 * o próprio log (RF-049) não gera uma nova entrada, para evitar ruído
 * recursivo.
 */
public final class AuditLogEntry {

    private final String id;
    private final String operatorUserId;
    private final String targetUserId;
    private final AuditAction action;
    private final String details;
    private final Instant createdAt;

    private AuditLogEntry(String id, String operatorUserId, String targetUserId, AuditAction action, String details, Instant createdAt) {
        this.id = id;
        this.operatorUserId = operatorUserId;
        this.targetUserId = targetUserId;
        this.action = action;
        this.details = details;
        this.createdAt = createdAt;
    }

    public static AuditLogEntry create(String id, String operatorUserId, String targetUserId, AuditAction action, String details) {
        return new AuditLogEntry(id, operatorUserId, targetUserId, action, details, Instant.now());
    }

    public static AuditLogEntry reconstitute(
            String id, String operatorUserId, String targetUserId, AuditAction action, String details, Instant createdAt) {
        return new AuditLogEntry(id, operatorUserId, targetUserId, action, details, createdAt);
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
