package com.financepulse.engine.domain.category.errors;

/** ADR-0017: preserva RN-002 — apagar uma categoria em uso deixaria transações com referência inválida. */
public class CategoryHasTransactionsException extends RuntimeException {

    public CategoryHasTransactionsException() {
        super("Não é possível excluir uma categoria que possui transações associadas.");
    }
}
