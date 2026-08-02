package com.financepulse.engine.domain.category.errors;

/** ADR-0017: preserva RN-002 — apagar uma categoria com subcategorias as deixaria órfãs. */
public class CategoryHasSubcategoriesException extends RuntimeException {

    public CategoryHasSubcategoriesException() {
        super("Não é possível excluir uma categoria que possui subcategorias.");
    }
}
