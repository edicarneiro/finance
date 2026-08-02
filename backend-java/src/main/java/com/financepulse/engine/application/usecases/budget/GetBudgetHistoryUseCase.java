package com.financepulse.engine.application.usecases.budget;

import com.financepulse.engine.application.ports.BudgetRepository;
import com.financepulse.engine.application.ports.Clock;
import com.financepulse.engine.application.ports.TransactionRepository;
import com.financepulse.engine.application.services.BudgetConsumptionCalculator;
import com.financepulse.engine.application.services.BudgetConsumptionCalculator.Consumption;
import com.financepulse.engine.application.services.BudgetPeriodCalculator;
import com.financepulse.engine.application.services.BudgetPeriodCalculator.PeriodRange;
import com.financepulse.engine.domain.budget.Budget;
import com.financepulse.engine.domain.budget.errors.BudgetNotFoundException;
import com.financepulse.engine.domain.transaction.Transaction;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * RF-029: desempenho de períodos anteriores. Recalculado sob demanda a
 * partir das transações já registradas — nenhum snapshot é persistido (ver
 * ADR-0018). Orçamentos CUSTOM não têm períodos anteriores (lista vazia).
 */
public class GetBudgetHistoryUseCase {

    static final int DEFAULT_PERIOD_COUNT = 6;
    static final int MAX_PERIOD_COUNT = 24;

    private final BudgetRepository budgetRepository;
    private final TransactionRepository transactionRepository;
    private final Clock clock;

    public GetBudgetHistoryUseCase(BudgetRepository budgetRepository, TransactionRepository transactionRepository, Clock clock) {
        this.budgetRepository = budgetRepository;
        this.transactionRepository = transactionRepository;
        this.clock = clock;
    }

    public Output execute(Input input) {
        Budget budget = budgetRepository.findByIdAndUserId(input.budgetId(), input.userId()).orElseThrow(BudgetNotFoundException::new);

        int periodCount = Math.min(Math.max(input.periodCount(), 1), MAX_PERIOD_COUNT);
        List<Transaction> categoryTransactions = transactionRepository.findAllByCategoryIdAndUserId(budget.getCategoryId(), input.userId());

        List<PeriodRange> periods = BudgetPeriodCalculator.previousPeriods(budget, clock.today(), periodCount);
        List<PeriodPerformance> performance = periods.stream().map(period -> toPerformance(budget, period, categoryTransactions)).toList();

        return new Output(performance);
    }

    private PeriodPerformance toPerformance(Budget budget, PeriodRange period, List<Transaction> categoryTransactions) {
        Consumption consumption = BudgetConsumptionCalculator.calculate(budget, period, categoryTransactions);

        return new PeriodPerformance(period.start(), period.end(), consumption.consumedAmount(), consumption.percentage());
    }

    public record Input(String userId, String budgetId, int periodCount) {

        public Input(String userId, String budgetId) {
            this(userId, budgetId, DEFAULT_PERIOD_COUNT);
        }
    }

    public record Output(List<PeriodPerformance> periods) {
    }

    public record PeriodPerformance(LocalDate periodStart, LocalDate periodEnd, BigDecimal consumedAmount, BigDecimal consumedPercentage) {
    }
}
