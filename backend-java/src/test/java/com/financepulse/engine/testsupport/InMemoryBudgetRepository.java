package com.financepulse.engine.testsupport;

import com.financepulse.engine.application.ports.BudgetRepository;
import com.financepulse.engine.domain.budget.Budget;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InMemoryBudgetRepository implements BudgetRepository {

    private final Map<String, Budget> budgetsById = new LinkedHashMap<>();

    @Override
    public Optional<Budget> findByIdAndUserId(String id, String userId) {
        return Optional.ofNullable(budgetsById.get(id)).filter(b -> b.getUserId().equals(userId));
    }

    @Override
    public List<Budget> findAllByUserId(String userId) {
        return budgetsById.values().stream().filter(b -> b.getUserId().equals(userId)).toList();
    }

    @Override
    public void save(Budget budget) {
        budgetsById.put(budget.getId(), budget);
    }

    @Override
    public void update(Budget budget) {
        budgetsById.put(budget.getId(), budget);
    }

    @Override
    public void deleteByIdAndUserId(String id, String userId) {
        budgetsById.computeIfPresent(id, (key, existing) -> existing.getUserId().equals(userId) ? null : existing);
    }
}
