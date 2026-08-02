package com.financepulse.engine.domain.goal.errors;

public class InvalidGoalTargetException extends RuntimeException {

    public InvalidGoalTargetException() {
        super("O valor-alvo da meta deve ser maior que zero.");
    }
}
