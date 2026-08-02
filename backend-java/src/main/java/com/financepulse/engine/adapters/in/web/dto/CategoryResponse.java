package com.financepulse.engine.adapters.in.web.dto;

import com.financepulse.engine.application.usecases.category.ListCategoriesUseCase.CategoryView;

public record CategoryResponse(String id, String name, String parentCategoryId) {

    public static CategoryResponse from(CategoryView view) {
        return new CategoryResponse(view.id(), view.name(), view.parentCategoryId());
    }
}
