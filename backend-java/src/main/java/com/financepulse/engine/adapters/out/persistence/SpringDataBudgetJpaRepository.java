package com.financepulse.engine.adapters.out.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataBudgetJpaRepository extends JpaRepository<BudgetJpaEntity, String> {

    Optional<BudgetJpaEntity> findByIdAndUserId(String id, String userId);

    List<BudgetJpaEntity> findAllByUserId(String userId);

    void deleteByIdAndUserId(String id, String userId);
}
