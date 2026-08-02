package com.financepulse.engine.application.usecases.budget;

import com.financepulse.engine.application.ports.BudgetRepository;
import com.financepulse.engine.domain.budget.Budget;
import com.financepulse.engine.domain.budget.errors.BudgetNotFoundException;
import java.math.BigDecimal;
import java.util.List;

/** RF-026: apenas limite e limiares de alerta são editáveis — categoria e tipo de período são imutáveis (ver ADR-0018). */
public class UpdateBudgetUseCase {

    private final BudgetRepository budgetRepository;

    public UpdateBudgetUseCase(BudgetRepository budgetRepository) {
        this.budgetRepository = budgetRepository;
    }

    public void execute(Input input) {
        Budget budget = budgetRepository.findByIdAndUserId(input.budgetId(), input.userId()).orElseThrow(BudgetNotFoundException::new);

        budgetRepository.update(budget.withLimitAndThresholds(input.limitAmount(), input.alertThresholds()));
    }

    public record Input(String userId, String budgetId, BigDecimal limitAmount, List<Integer> alertThresholds) {
    }
}
