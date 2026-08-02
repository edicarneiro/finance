package com.financepulse.engine.application.usecases.budget;

import com.financepulse.engine.application.ports.BudgetRepository;
import com.financepulse.engine.domain.budget.errors.BudgetNotFoundException;

/** Exclusão definitiva — nada referencia um orçamento (ao contrário de conta/categoria), então não há bloqueio de exclusão. */
public class DeleteBudgetUseCase {

    private final BudgetRepository budgetRepository;

    public DeleteBudgetUseCase(BudgetRepository budgetRepository) {
        this.budgetRepository = budgetRepository;
    }

    public void execute(Input input) {
        budgetRepository.findByIdAndUserId(input.budgetId(), input.userId()).orElseThrow(BudgetNotFoundException::new);

        budgetRepository.deleteByIdAndUserId(input.budgetId(), input.userId());
    }

    public record Input(String userId, String budgetId) {
    }
}
