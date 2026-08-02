package com.financepulse.engine.domain.category;

import com.financepulse.engine.domain.category.errors.InvalidCategoryNameException;

public final class CategoryPolicy {

    public static final int MAX_NAME_LENGTH = 100;

    private CategoryPolicy() {
    }

    public static String assertValidName(String rawName) {
        String trimmed = rawName == null ? "" : rawName.trim();

        if (trimmed.isEmpty() || trimmed.length() > MAX_NAME_LENGTH) {
            throw new InvalidCategoryNameException();
        }

        return trimmed;
    }
}
