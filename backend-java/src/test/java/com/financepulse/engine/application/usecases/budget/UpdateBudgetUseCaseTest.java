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
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UpdateBudgetUseCaseTest {

    private InMemoryBudgetRepository budgetRepository;
    private UpdateBudgetUseCase useCase;
    private String budgetId;

    @BeforeEach
    void setUp() {
        budgetRepository = new InMemoryBudgetRepository();
        InMemoryCategoryRepository categoryRepository = new InMemoryCategoryRepository();
        useCase = new UpdateBudgetUseCase(budgetRepository);

        Category category = Category.create("category-1", "user-1", "Alimentação", null);
        categoryRepository.save(category);
        budgetId = new CreateBudgetUseCase(budgetRepository, categoryRepository, new SequentialIdGenerator("budget"))
                .execute(new CreateBudgetUseCase.Input(
                        "user-1", category.getId(), new BigDecimal("500.00"), BudgetPeriodType.MONTHLY, null, null, null))
                .budgetId();
    }

    @Test
    void updatesLimitAndThresholdsOfAnOwnedBudget() {
        useCase.execute(new UpdateBudgetUseCase.Input("user-1", budgetId, new BigDecimal("700.00"), List.of(50)));

        var updated = budgetRepository.findByIdAndUserId(budgetId, "user-1").orElseThrow();
        assertThat(updated.getLimitAmount()).isEqualByComparingTo("700.00");
        assertThat(updated.getAlertThresholds()).containsExactly(50);
    }

    @Test
    void rejectsUpdatingANonExistentBudget() {
        assertThatThrownBy(() -> useCase.execute(new UpdateBudgetUseCase.Input("user-1", "ghost-budget", BigDecimal.TEN, null)))
                .isInstanceOf(BudgetNotFoundException.class);
    }

    @Test
    void rejectsUpdatingAnotherUsersBudgetWithTheSameErrorAsNotFound() {
        assertThatThrownBy(() -> useCase.execute(new UpdateBudgetUseCase.Input("another-user", budgetId, BigDecimal.TEN, null)))
                .isInstanceOf(BudgetNotFoundException.class);
    }
}
