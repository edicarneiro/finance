package com.financepulse.engine.domain.budget.errors;

public class InvalidAlertThresholdException extends RuntimeException {

    public InvalidAlertThresholdException() {
        super("Cada limiar de alerta deve ser um percentual entre 1 e 100.");
    }
}
