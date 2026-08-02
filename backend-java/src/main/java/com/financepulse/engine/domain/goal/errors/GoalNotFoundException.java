package com.financepulse.engine.domain.goal.errors;

public class GoalNotFoundException extends RuntimeException {

    public GoalNotFoundException() {
        super("Meta não encontrada.");
    }
}
