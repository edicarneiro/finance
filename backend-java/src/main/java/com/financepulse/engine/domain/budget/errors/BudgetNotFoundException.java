package com.financepulse.engine.domain.budget.errors;

public class BudgetNotFoundException extends RuntimeException {

    public BudgetNotFoundException() {
        super("Orçamento não encontrado.");
    }
}
