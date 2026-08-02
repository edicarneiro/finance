package com.financepulse.engine.domain.budget.errors;

public class InvalidBudgetLimitException extends RuntimeException {

    public InvalidBudgetLimitException() {
        super("O limite do orçamento deve ser maior que zero.");
    }
}
