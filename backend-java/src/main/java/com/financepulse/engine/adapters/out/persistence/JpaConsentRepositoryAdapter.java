package com.financepulse.engine.adapters.out.persistence;

import com.financepulse.engine.application.ports.ConsentRepository;
import com.financepulse.engine.domain.user.ConsentRecord;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class JpaConsentRepositoryAdapter implements ConsentRepository {

    private final SpringDataConsentJpaRepository jpaRepository;

    public JpaConsentRepositoryAdapter(SpringDataConsentJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public List<ConsentRecord> findAllByUserId(String userId) {
        return jpaRepository.findAllByUserId(userId).stream().map(this::toDomain).toList();
    }

    @Override
    public void save(ConsentRecord consentRecord) {
        jpaRepository.save(new ConsentRecordJpaEntity(
                consentRecord.getId(), consentRecord.getUserId(), consentRecord.getVersion(), consentRecord.getAcceptedAt()));
    }

    private ConsentRecord toDomain(ConsentRecordJpaEntity entity) {
        return ConsentRecord.reconstitute(entity.getId(), entity.getUserId(), entity.getVersion(), entity.getAcceptedAt());
    }
}
