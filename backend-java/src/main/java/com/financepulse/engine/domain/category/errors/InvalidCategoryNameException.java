package com.financepulse.engine.domain.category.errors;

public class InvalidCategoryNameException extends RuntimeException {

    public InvalidCategoryNameException() {
        super("O nome da categoria deve ter entre 1 e 100 caracteres.");
    }
}
