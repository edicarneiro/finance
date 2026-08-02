package com.financepulse.engine.adapters.out.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataConsentJpaRepository extends JpaRepository<ConsentRecordJpaEntity, String> {

    List<ConsentRecordJpaEntity> findAllByUserId(String userId);
}
