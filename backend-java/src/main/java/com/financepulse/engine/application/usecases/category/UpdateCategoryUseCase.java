package com.financepulse.engine.application.usecases.category;

import com.financepulse.engine.application.ports.CategoryRepository;
import com.financepulse.engine.domain.category.Category;
import com.financepulse.engine.domain.category.errors.CategoryNotFoundException;

/** RF-023: apenas o nome é editável — {@code parentCategoryId} é imutável (ver ADR-0017). */
public class UpdateCategoryUseCase {

    private final CategoryRepository categoryRepository;

    public UpdateCategoryUseCase(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public void execute(Input input) {
        Category category = categoryRepository
                .findByIdAndUserId(input.categoryId(), input.userId())
                .orElseThrow(CategoryNotFoundException::new);

        categoryRepository.update(category.withName(input.name()));
    }

    public record Input(String userId, String categoryId, String name) {
    }
}
