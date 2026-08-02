package com.financepulse.engine.application.ports;

import com.financepulse.engine.domain.budget.Budget;
import java.util.List;
import java.util.Optional;

public interface BudgetRepository {

    Optional<Budget> findByIdAndUserId(String id, String userId);

    List<Budget> findAllByUserId(String userId);

    void save(Budget budget);

    void update(Budget budget);

    void deleteByIdAndUserId(String id, String userId);
}
