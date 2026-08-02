package com.financepulse.engine.domain.goal.errors;

public class InvalidGoalNameException extends RuntimeException {

    public InvalidGoalNameException() {
        super("O nome da meta deve ter entre 1 e 100 caracteres.");
    }
}
