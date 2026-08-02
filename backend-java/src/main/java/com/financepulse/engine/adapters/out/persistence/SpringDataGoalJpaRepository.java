package com.financepulse.engine.adapters.out.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataGoalJpaRepository extends JpaRepository<GoalJpaEntity, String> {

    Optional<GoalJpaEntity> findByIdAndUserId(String id, String userId);

    List<GoalJpaEntity> findAllByUserId(String userId);

    void deleteByIdAndUserId(String id, String userId);
}
