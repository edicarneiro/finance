package com.financepulse.engine.application.ports;

import com.financepulse.engine.domain.goal.Goal;
import java.util.List;
import java.util.Optional;

public interface GoalRepository {

    Optional<Goal> findByIdAndUserId(String id, String userId);

    List<Goal> findAllByUserId(String userId);

    void save(Goal goal);

    void update(Goal goal);

    void deleteByIdAndUserId(String id, String userId);
}
