package com.financepulse.engine.testsupport;

import com.financepulse.engine.application.ports.GoalRepository;
import com.financepulse.engine.domain.goal.Goal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InMemoryGoalRepository implements GoalRepository {

    private final Map<String, Goal> goalsById = new LinkedHashMap<>();

    @Override
    public Optional<Goal> findByIdAndUserId(String id, String userId) {
        return Optional.ofNullable(goalsById.get(id)).filter(g -> g.getUserId().equals(userId));
    }

    @Override
    public List<Goal> findAllByUserId(String userId) {
        return goalsById.values().stream().filter(g -> g.getUserId().equals(userId)).toList();
    }

    @Override
    public void save(Goal goal) {
        goalsById.put(goal.getId(), goal);
    }

    @Override
    public void update(Goal goal) {
        goalsById.put(goal.getId(), goal);
    }

    @Override
    public void deleteByIdAndUserId(String id, String userId) {
        goalsById.computeIfPresent(id, (key, existing) -> existing.getUserId().equals(userId) ? null : existing);
    }
}
