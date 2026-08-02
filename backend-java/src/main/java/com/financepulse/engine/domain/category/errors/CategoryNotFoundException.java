package com.financepulse.engine.domain.category.errors;

public class CategoryNotFoundException extends RuntimeException {

    public CategoryNotFoundException() {
        super("Categoria não encontrada.");
    }
}
