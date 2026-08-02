package com.financepulse.engine.adapters.out.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataPulseScoreJpaRepository extends JpaRepository<PulseScoreSnapshotJpaEntity, String> {

    List<PulseScoreSnapshotJpaEntity> findAllByUserId(String userId);

    Optional<PulseScoreSnapshotJpaEntity> findByUserIdAndScoreDate(String userId, LocalDate scoreDate);
}
