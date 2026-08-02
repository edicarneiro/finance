package com.financepulse.engine.adapters.out.persistence;

import com.financepulse.engine.application.ports.PulseScoreRepository;
import com.financepulse.engine.domain.pulsescore.PulseScoreSnapshot;
import java.util.List;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JpaPulseScoreRepositoryAdapter implements PulseScoreRepository {

    private final SpringDataPulseScoreJpaRepository jpaRepository;

    public JpaPulseScoreRepositoryAdapter(SpringDataPulseScoreJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public List<PulseScoreSnapshot> findAllByUserId(String userId) {
        return jpaRepository.findAllByUserId(userId).stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional
    public void saveOrUpdate(PulseScoreSnapshot snapshot) {
        String id = jpaRepository
                .findByUserIdAndScoreDate(snapshot.getUserId(), snapshot.getScoreDate())
                .map(PulseScoreSnapshotJpaEntity::getId)
                .orElse(snapshot.getId());

        jpaRepository.save(toEntity(snapshot, id));
    }

    private PulseScoreSnapshotJpaEntity toEntity(PulseScoreSnapshot snapshot, String id) {
        return new PulseScoreSnapshotJpaEntity(
                id,
                snapshot.getUserId(),
                snapshot.getScoreDate(),
                snapshot.getOverallScore(),
                snapshot.getBudgetConsistencyScore().orElse(null),
                snapshot.getSavingsRateScore().orElse(null),
                snapshot.getSpendingDiversificationScore().orElse(null),
                snapshot.getBalanceTrendScore(),
                snapshot.getFormulaVersion(),
                snapshot.getCreatedAt());
    }

    private PulseScoreSnapshot toDomain(PulseScoreSnapshotJpaEntity entity) {
        return PulseScoreSnapshot.reconstitute(
                entity.getId(),
                entity.getUserId(),
                entity.getScoreDate(),
                entity.getOverallScore(),
                entity.getBudgetConsistencyScore(),
                entity.getSavingsRateScore(),
                entity.getSpendingDiversificationScore(),
                entity.getBalanceTrendScore(),
                entity.getFormulaVersion(),
                entity.getCreatedAt());
    }
}
