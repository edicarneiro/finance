package com.financepulse.engine.testsupport;

import com.financepulse.engine.application.ports.AuditLogRepository;
import com.financepulse.engine.domain.backoffice.AuditLogEntry;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class InMemoryAuditLogRepository implements AuditLogRepository {

    private final Map<String, AuditLogEntry> entriesById = new LinkedHashMap<>();

    @Override
    public List<AuditLogEntry> findAllByTargetUserId(String targetUserId) {
        return entriesById.values().stream().filter(entry -> entry.getTargetUserId().equals(targetUserId)).toList();
    }

    @Override
    public void save(AuditLogEntry entry) {
        entriesById.put(entry.getId(), entry);
    }
}
