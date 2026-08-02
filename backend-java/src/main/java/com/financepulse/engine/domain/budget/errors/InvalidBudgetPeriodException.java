package com.financepulse.engine.domain.budget.errors;

/** ADR-0018: CUSTOM exige customPeriodStart < customPeriodEnd; MONTHLY/WEEKLY não aceitam datas customizadas. */
public class InvalidBudgetPeriodException extends RuntimeException {

    public InvalidBudgetPeriodException(String message) {
        super(message);
    }
}
