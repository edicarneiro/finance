package com.financepulse.engine.application.usecases.budget;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.financepulse.engine.domain.budget.BudgetPeriodType;
import com.financepulse.engine.domain.budget.errors.BudgetNotFoundException;
import com.financepulse.engine.domain.category.Category;
import com.financepulse.engine.testsupport.InMemoryBudgetRepository;
import com.financepulse.engine.testsupport.InMemoryCategoryRepository;
import com.financepulse.engine.testsupport.SequentialIdGenerator;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DeleteBudgetUseCaseTest {

    private InMemoryBudgetRepository budgetRepository;
    private DeleteBudgetUseCase useCase;
    private String budgetId;

    @BeforeEach
    void setUp() {
        budgetRepository = new InMemoryBudgetRepository();
        InMemoryCategoryRepository categoryRepository = new InMemoryCategoryRepository();
        useCase = new DeleteBudgetUseCase(budgetRepository);

        Category category = Category.create("category-1", "user-1", "Alimentação", null);
        categoryRepository.save(category);
        budgetId = new CreateBudgetUseCase(budgetRepository, categoryRepository, new SequentialIdGenerator("budget"))
                .execute(new CreateBudgetUseCase.Input(
                        "user-1", category.getId(), new BigDecimal("500.00"), BudgetPeriodType.MONTHLY, null, null, null))
                .budgetId();
    }

    @Test
    void deletesAnOwnedBudget() {
        useCase.execute(new DeleteBudgetUseCase.Input("user-1", budgetId));

        assertThat(budgetRepository.findByIdAndUserId(budgetId, "user-1")).isEmpty();
    }

    @Test
    void rejectsDeletingANonExistentBudget() {
        assertThatThrownBy(() -> useCase.execute(new DeleteBudgetUseCase.Input("user-1", "ghost-budget")))
                .isInstanceOf(BudgetNotFoundException.class);
    }

    @Test
    void rejectsDeletingAnotherUsersBudgetWithTheSameErrorAsNotFound() {
        assertThatThrownBy(() -> useCase.execute(new DeleteBudgetUseCase.Input("another-user", budgetId)))
                .isInstanceOf(BudgetNotFoundException.class);

        assertThat(budgetRepository.findByIdAndUserId(budgetId, "user-1")).isPresent();
    }
}
