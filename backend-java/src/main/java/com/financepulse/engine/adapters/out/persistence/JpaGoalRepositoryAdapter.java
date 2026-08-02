package com.financepulse.engine.adapters.out.persistence;

import com.financepulse.engine.application.ports.GoalRepository;
import com.financepulse.engine.domain.goal.Goal;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JpaGoalRepositoryAdapter implements GoalRepository {

    private final SpringDataGoalJpaRepository jpaRepository;

    public JpaGoalRepositoryAdapter(SpringDataGoalJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<Goal> findByIdAndUserId(String id, String userId) {
        return jpaRepository.findByIdAndUserId(id, userId).map(this::toDomain);
    }

    @Override
    public List<Goal> findAllByUserId(String userId) {
        return jpaRepository.findAllByUserId(userId).stream().map(this::toDomain).toList();
    }

    @Override
    public void save(Goal goal) {
        jpaRepository.save(toEntity(goal));
    }

    @Override
    public void update(Goal goal) {
        jpaRepository.save(toEntity(goal));
    }

    @Override
    @Transactional
    public void deleteByIdAndUserId(String id, String userId) {
        jpaRepository.deleteByIdAndUserId(id, userId);
    }

    private GoalJpaEntity toEntity(Goal goal) {
        return new GoalJpaEntity(
                goal.getId(),
                goal.getUserId(),
                goal.getName(),
                goal.getTargetAmount(),
                goal.getDeadline(),
                goal.getAccountId().orElse(null),
                goal.getCategoryId().orElse(null),
                goal.getProgressAlertThresholds(),
                goal.getCreatedAt());
    }

    private Goal toDomain(GoalJpaEntity entity) {
        return Goal.reconstitute(
                entity.getId(),
                entity.getUserId(),
                entity.getName(),
                entity.getTargetAmount(),
                entity.getDeadline(),
                entity.getAccountId(),
                entity.getCategoryId(),
                entity.getProgressAlertThresholds(),
                entity.getCreatedAt());
    }
}
