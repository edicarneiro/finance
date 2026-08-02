package com.financepulse.engine.domain.category.errors;

/** ADR-0017: hierarquia limitada a 2 níveis — uma subcategoria não pode ter subcategorias. */
public class InvalidCategoryHierarchyException extends RuntimeException {

    public InvalidCategoryHierarchyException() {
        super("Uma subcategoria não pode ter subcategorias — a hierarquia é limitada a 2 níveis.");
    }
}
