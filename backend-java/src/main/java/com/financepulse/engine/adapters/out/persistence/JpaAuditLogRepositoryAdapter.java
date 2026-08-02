package com.financepulse.engine.adapters.out.persistence;

import com.financepulse.engine.application.ports.AuditLogRepository;
import com.financepulse.engine.domain.backoffice.AuditLogEntry;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class JpaAuditLogRepositoryAdapter implements AuditLogRepository {

    private final SpringDataAuditLogJpaRepository jpaRepository;

    public JpaAuditLogRepositoryAdapter(SpringDataAuditLogJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public List<AuditLogEntry> findAllByTargetUserId(String targetUserId) {
        return jpaRepository.findAllByTargetUserId(targetUserId).stream().map(this::toDomain).toList();
    }

    @Override
    public void save(AuditLogEntry entry) {
        jpaRepository.save(new AuditLogEntryJpaEntity(
                entry.getId(), entry.getOperatorUserId(), entry.getTargetUserId(), entry.getAction(), entry.getDetails(), entry.getCreatedAt()));
    }

    private AuditLogEntry toDomain(AuditLogEntryJpaEntity entity) {
        return AuditLogEntry.reconstitute(
                entity.getId(), entity.getOperatorUserId(), entity.getTargetUserId(), entity.getAction(), entity.getDetails(),
                entity.getCreatedAt());
    }
}
