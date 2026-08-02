package com.financepulse.engine.application.usecases.budget;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.financepulse.engine.domain.budget.BudgetPeriodType;
import com.financepulse.engine.domain.category.Category;
import com.financepulse.engine.domain.category.errors.CategoryNotFoundException;
import com.financepulse.engine.testsupport.InMemoryBudgetRepository;
import com.financepulse.engine.testsupport.InMemoryCategoryRepository;
import com.financepulse.engine.testsupport.SequentialIdGenerator;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CreateBudgetUseCaseTest {

    private InMemoryBudgetRepository budgetRepository;
    private InMemoryCategoryRepository categoryRepository;
    private CreateBudgetUseCase useCase;
    private String categoryId;

    @BeforeEach
    void setUp() {
        budgetRepository = new InMemoryBudgetRepository();
        categoryRepository = new InMemoryCategoryRepository();
        useCase = new CreateBudgetUseCase(budgetRepository, categoryRepository, new SequentialIdGenerator("budget"));

        Category category = Category.create("category-1", "user-1", "Alimentação", null);
        categoryRepository.save(category);
        categoryId = category.getId();
    }

    @Test
    void createsAMonthlyBudgetForAnOwnedCategory() {
        CreateBudgetUseCase.Output result = useCase.execute(new CreateBudgetUseCase.Input(
                "user-1", categoryId, new BigDecimal("500.00"), BudgetPeriodType.MONTHLY, null, null, null));

        assertThat(result.budgetId()).isEqualTo("budget-1");
        assertThat(budgetRepository.findByIdAndUserId("budget-1", "user-1")).isPresent();
    }

    @Test
    void rejectsANonExistentCategory() {
        assertThatThrownBy(() -> useCase.execute(new CreateBudgetUseCase.Input(
                        "user-1", "ghost-category", BigDecimal.TEN, BudgetPeriodType.MONTHLY, null, null, null)))
                .isInstanceOf(CategoryNotFoundException.class);
    }

    @Test
    void rejectsACategoryBelongingToAnotherUser() {
        assertThatThrownBy(() -> useCase.execute(new CreateBudgetUseCase.Input(
                        "another-user", categoryId, BigDecimal.TEN, BudgetPeriodType.MONTHLY, null, null, null)))
                .isInstanceOf(CategoryNotFoundException.class);
    }
}
