package com.financepulse.engine.domain.user;

import com.financepulse.engine.domain.user.errors.InvalidConsentVersionException;
import java.time.Instant;

/**
 * RF-046 (ver ADR-0023, replica ADR-0008 do backend TypeScript): trilha
 * auditável append-only — nunca atualizado ou apagado. {@code version}
 * identifica a versão dos termos/política aceitos, fornecida pelo cliente;
 * o conteúdo jurídico da política em si não é modelado nem armazenado.
 */
public final class ConsentRecord {

    private final String id;
    private final String userId;
    private final String version;
    private final Instant acceptedAt;

    private ConsentRecord(String id, String userId, String version, Instant acceptedAt) {
        this.id = id;
        this.userId = userId;
        this.version = version;
        this.acceptedAt = acceptedAt;
    }

    public static ConsentRecord create(String id, String userId, String version) {
        String validatedVersion = assertValidVersion(version);
        return new ConsentRecord(id, userId, validatedVersion, Instant.now());
    }

    public static ConsentRecord reconstitute(String id, String userId, String version, Instant acceptedAt) {
        return new ConsentRecord(id, userId, version, acceptedAt);
    }

    private static String assertValidVersion(String rawVersion) {
        String trimmed = rawVersion == null ? "" : rawVersion.trim();
        if (trimmed.isEmpty()) {
            throw new InvalidConsentVersionException();
        }
        return trimmed;
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
