package com.financepulse.engine.application.usecases.category;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.financepulse.engine.domain.category.errors.CategoryNotFoundException;
import com.financepulse.engine.domain.category.errors.InvalidCategoryNameException;
import com.financepulse.engine.testsupport.InMemoryCategoryRepository;
import com.financepulse.engine.testsupport.SequentialIdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UpdateCategoryUseCaseTest {

    private InMemoryCategoryRepository categoryRepository;
    private UpdateCategoryUseCase useCase;
    private String categoryId;

    @BeforeEach
    void setUp() {
        categoryRepository = new InMemoryCategoryRepository();
        useCase = new UpdateCategoryUseCase(categoryRepository);

        categoryId = new CreateCategoryUseCase(categoryRepository, new SequentialIdGenerator("category"))
                .execute(new CreateCategoryUseCase.Input("user-1", "Alimentação", null))
                .categoryId();
    }

    @Test
    void renamesAnOwnedCategory() {
        useCase.execute(new UpdateCategoryUseCase.Input("user-1", categoryId, "Comida"));

        assertThat(categoryRepository.findByIdAndUserId(categoryId, "user-1").orElseThrow().getName()).isEqualTo("Comida");
    }

    @Test
    void rejectsAnInvalidName() {
        assertThatThrownBy(() -> useCase.execute(new UpdateCategoryUseCase.Input("user-1", categoryId, "   ")))
                .isInstanceOf(InvalidCategoryNameException.class);
    }

    @Test
    void rejectsUpdatingANonExistentCategory() {
        assertThatThrownBy(() -> useCase.execute(new UpdateCategoryUseCase.Input("user-1", "ghost-category", "Novo Nome")))
                .isInstanceOf(CategoryNotFoundException.class);
    }

    @Test
    void rejectsUpdatingAnotherUsersCategoryWithTheSameErrorAsNotFound() {
        assertThatThrownBy(() -> useCase.execute(new UpdateCategoryUseCase.Input("another-user", categoryId, "Novo Nome")))
                .isInstanceOf(CategoryNotFoundException.class);
    }
}
