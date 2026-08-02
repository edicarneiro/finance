package com.financepulse.engine.application.usecases.category;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.financepulse.engine.domain.category.errors.CategoryNotFoundException;
import com.financepulse.engine.domain.category.errors.InvalidCategoryHierarchyException;
import com.financepulse.engine.testsupport.InMemoryCategoryRepository;
import com.financepulse.engine.testsupport.SequentialIdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CreateCategoryUseCaseTest {

    private InMemoryCategoryRepository categoryRepository;
    private CreateCategoryUseCase useCase;

    @BeforeEach
    void setUp() {
        categoryRepository = new InMemoryCategoryRepository();
        useCase = new CreateCategoryUseCase(categoryRepository, new SequentialIdGenerator("category"));
    }

    @Test
    void createsATopLevelCategory() {
        CreateCategoryUseCase.Output result = useCase.execute(new CreateCategoryUseCase.Input("user-1", "Alimentação", null));

        assertThat(result.categoryId()).isEqualTo("category-1");
        assertThat(categoryRepository.findByIdAndUserId("category-1", "user-1")).isPresent();
    }

    @Test
    void createsASubcategoryOfAnExistingTopLevelCategory() {
        String parentId =
                useCase.execute(new CreateCategoryUseCase.Input("user-1", "Alimentação", null)).categoryId();

        CreateCategoryUseCase.Output result = useCase.execute(new CreateCategoryUseCase.Input("user-1", "Restaurante", parentId));

        assertThat(categoryRepository.findByIdAndUserId(result.categoryId(), "user-1").orElseThrow().getParentCategoryId())
                .contains(parentId);
    }

    @Test
    void rejectsANonExistentParent() {
        assertThatThrownBy(() -> useCase.execute(new CreateCategoryUseCase.Input("user-1", "Restaurante", "ghost-parent")))
                .isInstanceOf(CategoryNotFoundException.class);
    }

    @Test
    void rejectsAParentBelongingToAnotherUser() {
        String parentId =
                useCase.execute(new CreateCategoryUseCase.Input("user-1", "Alimentação", null)).categoryId();

        assertThatThrownBy(() -> useCase.execute(new CreateCategoryUseCase.Input("another-user", "Restaurante", parentId)))
                .isInstanceOf(CategoryNotFoundException.class);
    }

    @Test
    void rejectsCreatingASubcategoryOfASubcategory() {
        String parentId =
                useCase.execute(new CreateCategoryUseCase.Input("user-1", "Alimentação", null)).categoryId();
        String subcategoryId =
                useCase.execute(new CreateCategoryUseCase.Input("user-1", "Restaurante", parentId)).categoryId();

        assertThatThrownBy(() -> useCase.execute(new CreateCategoryUseCase.Input("user-1", "Fast-food", subcategoryId)))
                .isInstanceOf(InvalidCategoryHierarchyException.class);
    }
}
