package com.financepulse.engine.adapters.out.persistence;

import com.financepulse.engine.application.ports.BudgetRepository;
import com.financepulse.engine.domain.budget.Budget;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JpaBudgetRepositoryAdapter implements BudgetRepository {

    private final SpringDataBudgetJpaRepository jpaRepository;

    public JpaBudgetRepositoryAdapter(SpringDataBudgetJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<Budget> findByIdAndUserId(String id, String userId) {
        return jpaRepository.findByIdAndUserId(id, userId).map(this::toDomain);
    }

    @Override
    public List<Budget> findAllByUserId(String userId) {
        return jpaRepository.findAllByUserId(userId).stream().map(this::toDomain).toList();
    }

    @Override
    public void save(Budget budget) {
        jpaRepository.save(toEntity(budget));
    }

    @Override
    public void update(Budget budget) {
        jpaRepository.save(toEntity(budget));
    }

    @Override
    @Transactional
    public void deleteByIdAndUserId(String id, String userId) {
        jpaRepository.deleteByIdAndUserId(id, userId);
    }

    private BudgetJpaEntity toEntity(Budget budget) {
        return new BudgetJpaEntity(
                budget.getId(),
                budget.getUserId(),
                budget.getCategoryId(),
                budget.getLimitAmount(),
                budget.getPeriodType(),
                budget.getCustomPeriodStart().orElse(null),
                budget.getCustomPeriodEnd().orElse(null),
                budget.getAlertThresholds(),
                budget.getCreatedAt());
    }

    private Budget toDomain(BudgetJpaEntity entity) {
        return Budget.reconstitute(
                entity.getId(),
                entity.getUserId(),
                entity.getCategoryId(),
                entity.getLimitAmount(),
                entity.getPeriodType(),
                entity.getCustomPeriodStart(),
                entity.getCustomPeriodEnd(),
                entity.getAlertThresholds(),
                entity.getCreatedAt());
    }
}
