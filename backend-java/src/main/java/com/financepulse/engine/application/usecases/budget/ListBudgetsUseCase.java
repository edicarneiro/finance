package com.financepulse.engine.application.usecases.budget;

import com.financepulse.engine.application.ports.BudgetRepository;
import com.financepulse.engine.application.ports.Clock;
import com.financepulse.engine.application.ports.TransactionRepository;
import com.financepulse.engine.application.services.BudgetConsumptionCalculator;
import com.financepulse.engine.application.services.BudgetConsumptionCalculator.Consumption;
import com.financepulse.engine.application.services.BudgetPeriodCalculator;
import com.financepulse.engine.domain.budget.Budget;
import com.financepulse.engine.domain.budget.BudgetPeriodType;
import com.financepulse.engine.domain.transaction.Transaction;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** RF-027 (consumo em tempo real do período vigente) + RF-028 (sinal de limiares ultrapassados, ver ADR-0018). */
public class ListBudgetsUseCase {

    private final BudgetRepository budgetRepository;
    private final TransactionRepository transactionRepository;
    private final Clock clock;

    public ListBudgetsUseCase(BudgetRepository budgetRepository, TransactionRepository transactionRepository, Clock clock) {
        this.budgetRepository = budgetRepository;
        this.transactionRepository = transactionRepository;
        this.clock = clock;
    }

    public Output execute(Input input) {
        LocalDate today = clock.today();

        List<BudgetView> views = budgetRepository.findAllByUserId(input.userId()).stream()
                .map(budget -> toView(budget, today))
                .toList();

        return new Output(views);
    }

    private BudgetView toView(Budget budget, LocalDate today) {
        var period = BudgetPeriodCalculator.currentPeriod(budget, today);
        List<Transaction> categoryTransactions =
                transactionRepository.findAllByCategoryIdAndUserId(budget.getCategoryId(), budget.getUserId());
        Consumption consumption = BudgetConsumptionCalculator.calculate(budget, period, categoryTransactions);

        return new BudgetView(
                budget.getId(),
                budget.getCategoryId(),
                budget.getLimitAmount(),
                budget.getPeriodType(),
                budget.getAlertThresholds(),
                period.start(),
                period.end(),
                consumption.consumedAmount(),
                consumption.percentage(),
                consumption.thresholdsCrossed());
    }

    public record Input(String userId) {
    }

    public record Output(List<BudgetView> budgets) {
    }

    public record BudgetView(
            String id,
            String categoryId,
            BigDecimal limitAmount,
            BudgetPeriodType periodType,
            List<Integer> alertThresholds,
            LocalDate periodStart,
            LocalDate periodEnd,
            BigDecimal consumedAmount,
            BigDecimal consumedPercentage,
            List<Integer> thresholdsCrossed) {
    }
}
