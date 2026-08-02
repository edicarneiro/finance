package com.financepulse.engine.application.ports;

import com.financepulse.engine.domain.backoffice.AuditLogEntry;
import java.util.List;

/** RF-048: append-only — sem update/delete. Escopado por targetUserId (o usuário cujos dados foram acessados). */
public interface AuditLogRepository {

    List<AuditLogEntry> findAllByTargetUserId(String targetUserId);

    void save(AuditLogEntry entry);
}
