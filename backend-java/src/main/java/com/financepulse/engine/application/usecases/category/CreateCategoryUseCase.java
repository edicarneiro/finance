package com.financepulse.engine.application.usecases.category;

import com.financepulse.engine.application.ports.CategoryRepository;
import com.financepulse.engine.application.ports.IdGenerator;
import com.financepulse.engine.domain.category.Category;
import com.financepulse.engine.domain.category.errors.CategoryNotFoundException;
import com.financepulse.engine.domain.category.errors.InvalidCategoryHierarchyException;

/** RF-023. Hierarquia limitada a 2 níveis — ver ADR-0017. */
public class CreateCategoryUseCase {

    private final CategoryRepository categoryRepository;
    private final IdGenerator idGenerator;

    public CreateCategoryUseCase(CategoryRepository categoryRepository, IdGenerator idGenerator) {
        this.categoryRepository = categoryRepository;
        this.idGenerator = idGenerator;
    }

    public Output execute(Input input) {
        if (input.parentCategoryId() != null) {
            Category parent = categoryRepository
                    .findByIdAndUserId(input.parentCategoryId(), input.userId())
                    .orElseThrow(CategoryNotFoundException::new);
            if (parent.isSubcategory()) {
                throw new InvalidCategoryHierarchyException();
            }
        }

        Category category = Category.create(idGenerator.generate(), input.userId(), input.name(), input.parentCategoryId());

        categoryRepository.save(category);

        return new Output(category.getId());
    }

    public record Input(String userId, String name, String parentCategoryId) {
    }

    public record Output(String categoryId) {
    }
}
