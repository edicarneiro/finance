package com.financepulse.engine.application.usecases.report;

import com.financepulse.engine.application.ports.CategoryRepository;
import com.financepulse.engine.application.ports.TransactionRepository;
import com.financepulse.engine.application.services.ReportPeriod;
import com.financepulse.engine.application.services.SpendingByCategoryCalculator;
import com.financepulse.engine.domain.category.Category;
import com.financepulse.engine.domain.transaction.Transaction;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** RF-037: gastos por categoria em um período selecionável — sempre recalculado sob demanda, nenhum valor persistido. */
public class GetSpendingByCategoryReportUseCase {

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;

    public GetSpendingByCategoryReportUseCase(TransactionRepository transactionRepository, CategoryRepository categoryRepository) {
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
    }

    public Output execute(Input input) {
        ReportPeriod period = new ReportPeriod(input.startDate(), input.endDate());

        List<Transaction> transactionsInPeriod = transactionRepository.findAllByUserId(input.userId()).stream()
                .filter(transaction -> period.contains(transaction.getDate()))
                .toList();

        SpendingByCategoryCalculator.Result result = SpendingByCategoryCalculator.calculate(transactionsInPeriod);
        Map<String, String> categoryNames =
                categoryRepository.findAllByUserId(input.userId()).stream().collect(Collectors.toMap(Category::getId, Category::getName));

        List<CategoryAmount> categories = result.categories().stream()
                .map(category -> new CategoryAmount(
                        category.categoryId(), categoryNames.getOrDefault(category.categoryId(), category.categoryId()), category.amount(),
                        category.percentage()))
                .toList();

        return new Output(period.start(), period.end(), result.totalExpense(), categories);
    }

    public record Input(String userId, LocalDate startDate, LocalDate endDate) {
    }

    public record Output(LocalDate startDate, LocalDate endDate, BigDecimal totalExpense, List<CategoryAmount> categories) {
    }

    public record CategoryAmount(String categoryId, String categoryName, BigDecimal amount, BigDecimal percentage) {
    }
}
