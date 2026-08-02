package com.financepulse.engine.application.usecases.report;

import com.financepulse.engine.application.ports.CategoryRepository;
import com.financepulse.engine.application.ports.TransactionRepository;
import com.financepulse.engine.application.services.PeriodComparisonCalculator;
import com.financepulse.engine.application.services.ReportPeriod;
import com.financepulse.engine.domain.category.Category;
import com.financepulse.engine.domain.transaction.Transaction;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * RF-038: comparativo entre dois períodos quaisquer (ex.: mês atual vs. mês
 * anterior é responsabilidade do cliente calcular as datas — ver ADR-0021).
 */
public class GetPeriodComparisonReportUseCase {

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;

    public GetPeriodComparisonReportUseCase(TransactionRepository transactionRepository, CategoryRepository categoryRepository) {
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
    }

    public Output execute(Input input) {
        ReportPeriod periodA = new ReportPeriod(input.periodAStart(), input.periodAEnd());
        ReportPeriod periodB = new ReportPeriod(input.periodBStart(), input.periodBEnd());

        List<Transaction> allTransactions = transactionRepository.findAllByUserId(input.userId());
        List<Transaction> periodATransactions = allTransactions.stream().filter(t -> periodA.contains(t.getDate())).toList();
        List<Transaction> periodBTransactions = allTransactions.stream().filter(t -> periodB.contains(t.getDate())).toList();

        PeriodComparisonCalculator.Result result = PeriodComparisonCalculator.calculate(periodATransactions, periodBTransactions);
        Map<String, String> categoryNames =
                categoryRepository.findAllByUserId(input.userId()).stream().collect(Collectors.toMap(Category::getId, Category::getName));

        List<CategoryComparison> categories = result.categoryComparisons().stream()
                .map(comparison -> new CategoryComparison(
                        comparison.categoryId(),
                        categoryNames.getOrDefault(comparison.categoryId(), comparison.categoryId()),
                        comparison.amountA(),
                        comparison.amountB(),
                        comparison.delta(),
                        comparison.percentageChange()))
                .toList();

        return new Output(
                new PeriodSummary(periodA.start(), periodA.end(), result.periodA().totalIncome(), result.periodA().totalExpense(), result.periodA().net()),
                new PeriodSummary(periodB.start(), periodB.end(), result.periodB().totalIncome(), result.periodB().totalExpense(), result.periodB().net()),
                categories);
    }

    public record Input(String userId, LocalDate periodAStart, LocalDate periodAEnd, LocalDate periodBStart, LocalDate periodBEnd) {
    }

    public record Output(PeriodSummary periodA, PeriodSummary periodB, List<CategoryComparison> categoryComparisons) {
    }

    public record PeriodSummary(LocalDate startDate, LocalDate endDate, BigDecimal totalIncome, BigDecimal totalExpense, BigDecimal net) {
    }

    public record CategoryComparison(
            String categoryId, String categoryName, BigDecimal amountPeriodA, BigDecimal amountPeriodB, BigDecimal delta, BigDecimal percentageChange) {
    }
}
