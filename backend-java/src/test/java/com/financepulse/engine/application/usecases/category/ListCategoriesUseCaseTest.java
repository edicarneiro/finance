package com.financepulse.engine.application.usecases.category;

import static org.assertj.core.api.Assertions.assertThat;

import com.financepulse.engine.testsupport.InMemoryCategoryRepository;
import com.financepulse.engine.testsupport.SequentialIdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ListCategoriesUseCaseTest {

    private InMemoryCategoryRepository categoryRepository;
    private ListCategoriesUseCase useCase;

    @BeforeEach
    void setUp() {
        categoryRepository = new InMemoryCategoryRepository();
        useCase = new ListCategoriesUseCase(categoryRepository, new SequentialIdGenerator("category"));
    }

    @Test
    void seedsTheDefaultCategoriesOnFirstAccessForAUserWithNone() {
        ListCategoriesUseCase.Output result = useCase.execute(new ListCategoriesUseCase.Input("user-1"));

        assertThat(result.categories()).hasSize(ListCategoriesUseCase.DEFAULT_CATEGORY_NAMES.size());
        assertThat(result.categories()).extracting(ListCategoriesUseCase.CategoryView::name)
                .containsExactlyElementsOf(ListCategoriesUseCase.DEFAULT_CATEGORY_NAMES);
    }

    @Test
    void doesNotReseedForAUserWhoAlreadyHasCategories() {
        useCase.execute(new ListCategoriesUseCase.Input("user-1"));

        ListCategoriesUseCase.Output result = useCase.execute(new ListCategoriesUseCase.Input("user-1"));

        assertThat(result.categories()).hasSize(ListCategoriesUseCase.DEFAULT_CATEGORY_NAMES.size());
        assertThat(categoryRepository.findAllByUserId("user-1")).hasSize(ListCategoriesUseCase.DEFAULT_CATEGORY_NAMES.size());
    }

    @Test
    void seedsIndependentlyForEachUser() {
        useCase.execute(new ListCategoriesUseCase.Input("user-1"));

        ListCategoriesUseCase.Output result = useCase.execute(new ListCategoriesUseCase.Input("user-2"));

        assertThat(result.categories()).hasSize(ListCategoriesUseCase.DEFAULT_CATEGORY_NAMES.size());
        assertThat(categoryRepository.findAllByUserId("user-1")).isNotEqualTo(categoryRepository.findAllByUserId("user-2"));
    }
}
