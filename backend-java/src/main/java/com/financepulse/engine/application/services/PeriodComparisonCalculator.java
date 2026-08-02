package com.financepulse.engine.application.services;

import com.financepulse.engine.domain.transaction.Transaction;
import com.financepulse.engine.domain.transaction.TransactionType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * RF-038: compara totais e gastos por categoria entre dois períodos —
 * função pura, sem I/O. O chamador já filtra as transações de cada período
 * (ver {@link ReportPeriod}); esta classe não sabe nada sobre datas.
 */
public final class PeriodComparisonCalculator {

    private PeriodComparisonCalculator() {
    }

    public static Result calculate(List<Transaction> periodATransactions, List<Transaction> periodBTransactions) {
        PeriodTotals totalsA = totals(periodATransactions);
        PeriodTotals totalsB = totals(periodBTransactions);
        List<CategoryComparison> categoryComparisons = compareCategories(periodATransactions, periodBTransactions);

        return new Result(totalsA, totalsB, categoryComparisons);
    }

    private static PeriodTotals totals(List<Transaction> transactions) {
        BigDecimal income = sumByType(transactions, TransactionType.INCOME);
        BigDecimal expense = sumByType(transactions, TransactionType.EXPENSE);

        return new PeriodTotals(income, expense, income.subtract(expense));
    }

    private static List<CategoryComparison> compareCategories(List<Transaction> periodATransactions, List<Transaction> periodBTransactions) {
        Map<String, BigDecimal> expenseA = expenseByCategory(periodATransactions);
        Map<String, BigDecimal> expenseB = expenseByCategory(periodBTransactions);

        Set<String> categoryIds = new HashSet<>();
        categoryIds.addAll(expenseA.keySet());
        categoryIds.addAll(expenseB.keySet());

        return categoryIds.stream()
                .map(categoryId -> {
                    BigDecimal amountA = expenseA.getOrDefault(categoryId, BigDecimal.ZERO);
                    BigDecimal amountB = expenseB.getOrDefault(categoryId, BigDecimal.ZERO);
                    BigDecimal delta = amountB.subtract(amountA);
                    BigDecimal percentageChange = amountA.signum() == 0
                            ? null
                            : delta.divide(amountA, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));

                    return new CategoryComparison(categoryId, amountA, amountB, delta, percentageChange);
                })
                .sorted(Comparator.comparing(CategoryComparison::amountB).reversed())
                .toList();
    }

    private static Map<String, BigDecimal> expenseByCategory(List<Transaction> transactions) {
        return transactions.stream()
                .filter(transaction -> transaction.getType() == TransactionType.EXPENSE)
                .collect(Collectors.groupingBy(Transaction::getCategoryId, Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)));
    }

    private static BigDecimal sumByType(List<Transaction> transactions, TransactionType type) {
        return transactions.stream().filter(t -> t.getType() == type).map(Transaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public record PeriodTotals(BigDecimal totalIncome, BigDecimal totalExpense, BigDecimal net) {
    }

    /** {@code percentageChange} é {@code null} quando {@code amountA} é zero (variação percentual indefinida a partir de uma base zero). */
    public record CategoryComparison(String categoryId, BigDecimal amountA, BigDecimal amountB, BigDecimal delta, BigDecimal percentageChange) {
    }

    public record Result(PeriodTotals periodA, PeriodTotals periodB, List<CategoryComparison> categoryComparisons) {
    }
}
