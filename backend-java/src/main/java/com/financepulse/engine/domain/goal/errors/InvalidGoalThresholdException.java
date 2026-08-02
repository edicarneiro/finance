package com.financepulse.engine.domain.goal.errors;

public class InvalidGoalThresholdException extends RuntimeException {

    public InvalidGoalThresholdException() {
        super("Cada limiar de progresso deve ser um percentual entre 1 e 100.");
    }
}
