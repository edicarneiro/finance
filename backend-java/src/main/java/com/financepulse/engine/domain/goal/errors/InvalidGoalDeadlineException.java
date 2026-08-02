package com.financepulse.engine.domain.goal.errors;

public class InvalidGoalDeadlineException extends RuntimeException {

    public InvalidGoalDeadlineException() {
        super("O prazo da meta deve ser uma data futura.");
    }
}
