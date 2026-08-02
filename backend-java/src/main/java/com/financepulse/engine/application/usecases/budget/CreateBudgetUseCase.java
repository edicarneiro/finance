package com.financepulse.engine.application.usecases.budget;

import com.financepulse.engine.application.ports.BudgetRepository;
import com.financepulse.engine.application.ports.CategoryRepository;
import com.financepulse.engine.application.ports.IdGenerator;
import com.financepulse.engine.domain.budget.Budget;
import com.financepulse.engine.domain.budget.BudgetPeriodType;
import com.financepulse.engine.domain.category.errors.CategoryNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class CreateBudgetUseCase {

    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;
    private final IdGenerator idGenerator;

    public CreateBudgetUseCase(BudgetRepository budgetRepository, CategoryRepository categoryRepository, IdGenerator idGenerator) {
        this.budgetRepository = budgetRepository;
        this.categoryRepository = categoryRepository;
        this.idGenerator = idGenerator;
    }

    public Output execute(Input input) {
        categoryRepository.findByIdAndUserId(input.categoryId(), input.userId()).orElseThrow(CategoryNotFoundException::new);

        Budget budget = Budget.create(
                idGenerator.generate(),
                input.userId(),
                input.categoryId(),
                input.limitAmount(),
                input.periodType(),
                input.customPeriodStart(),
                input.customPeriodEnd(),
                input.alertThresholds());

        budgetRepository.save(budget);

        return new Output(budget.getId());
    }

    public record Input(
            String userId,
            String categoryId,
            BigDecimal limitAmount,
            BudgetPeriodType periodType,
            LocalDate customPeriodStart,
            LocalDate customPeriodEnd,
            List<Integer> alertThresholds) {
    }

    public record Output(String budgetId) {
    }
}
