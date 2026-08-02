package com.financepulse.engine.application.services;

import com.financepulse.engine.domain.transaction.Transaction;
import com.financepulse.engine.domain.transaction.TransactionType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * RF-033/RF-037: agrupa transações de despesa (EXPENSE) por categoria e
 * calcula o percentual de cada uma sobre o total — função pura, sem I/O.
 * Extraída nesta fase (ver ADR-0021) de uma implementação até então inline
 * em {@code GetDashboardUseCase} (Fase 8), reaproveitada agora também pelos
 * relatórios (Fase 9) — mesmo cálculo, duas fontes de transações diferentes
 * (janela rolante do dashboard vs. período explícito do relatório).
 */
public final class SpendingByCategoryCalculator {

    private SpendingByCategoryCalculator() {
    }

    public static Result calculate(List<Transaction> transactions) {
        Map<String, BigDecimal> byCategory = transactions.stream()
                .filter(transaction -> transaction.getType() == TransactionType.EXPENSE)
                .collect(Collectors.groupingBy(Transaction::getCategoryId, Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)));

        BigDecimal total = byCategory.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);

        List<CategoryAmount> categories = byCategory.entrySet().stream()
                .map(entry -> new CategoryAmount(entry.getKey(), entry.getValue(), percentageOf(entry.getValue(), total)))
                .sorted(Comparator.comparing(CategoryAmount::amount).reversed())
                .toList();

        return new Result(total, categories);
    }

    private static BigDecimal percentageOf(BigDecimal amount, BigDecimal total) {
        if (total.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return amount.divide(total, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
    }

    public record CategoryAmount(String categoryId, BigDecimal amount, BigDecimal percentage) {
    }

    public record Result(BigDecimal totalExpense, List<CategoryAmount> categories) {
    }
}
