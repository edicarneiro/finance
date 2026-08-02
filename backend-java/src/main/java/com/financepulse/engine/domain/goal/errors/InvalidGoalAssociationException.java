package com.financepulse.engine.domain.goal.errors;

/** ADR-0019: exatamente uma de accountId/categoryId deve ser informada — nunca ambas, nunca nenhuma. */
public class InvalidGoalAssociationException extends RuntimeException {

    public InvalidGoalAssociationException() {
        super("Informe exatamente uma associação para a meta: conta ou categoria, nunca ambas nem nenhuma.");
    }
}
