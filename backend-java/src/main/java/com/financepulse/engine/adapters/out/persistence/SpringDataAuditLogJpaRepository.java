package com.financepulse.engine.adapters.out.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataAuditLogJpaRepository extends JpaRepository<AuditLogEntryJpaEntity, String> {

    List<AuditLogEntryJpaEntity> findAllByTargetUserId(String targetUserId);
}
