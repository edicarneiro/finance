package com.financepulse.engine.application.usecases.report;

import com.financepulse.engine.application.ports.AccountRepository;
import com.financepulse.engine.application.ports.CategoryRepository;
import com.financepulse.engine.application.ports.TransactionRepository;
import com.financepulse.engine.application.services.ReportPeriod;
import com.financepulse.engine.domain.account.Account;
import com.financepulse.engine.domain.category.Category;
import com.financepulse.engine.domain.transaction.Transaction;
import com.financepulse.engine.domain.transaction.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** RF-039: dados de transações de um período, enriquecidos com nome de conta/categoria — usado pela exportação CSV. */
public class GetTransactionsForPeriodUseCase {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;

    public GetTransactionsForPeriodUseCase(
            TransactionRepository transactionRepository, AccountRepository accountRepository, CategoryRepository categoryRepository) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.categoryRepository = categoryRepository;
    }

    public Output execute(Input input) {
        ReportPeriod period = new ReportPeriod(input.startDate(), input.endDate());

        Map<String, String> accountNames =
                accountRepository.findAllByUserId(input.userId()).stream().collect(Collectors.toMap(Account::getId, Account::getName));
        Map<String, String> categoryNames =
                categoryRepository.findAllByUserId(input.userId()).stream().collect(Collectors.toMap(Category::getId, Category::getName));

        List<TransactionRow> rows = transactionRepository.findAllByUserId(input.userId()).stream()
                .filter(transaction -> period.contains(transaction.getDate()))
                .sorted(Comparator.comparing(Transaction::getDate))
                .map(transaction -> new TransactionRow(
                        transaction.getDate(),
                        accountNames.getOrDefault(transaction.getAccountId(), transaction.getAccountId()),
                        categoryNames.getOrDefault(transaction.getCategoryId(), transaction.getCategoryId()),
                        transaction.getType(),
                        transaction.getAmount(),
                        transaction.getDescription(),
                        transaction.getTags()))
                .toList();

        return new Output(period.start(), period.end(), rows);
    }

    public record Input(String userId, LocalDate startDate, LocalDate endDate) {
    }

    public record Output(LocalDate startDate, LocalDate endDate, List<TransactionRow> transactions) {
    }

    public record TransactionRow(
            LocalDate date, String accountName, String categoryName, TransactionType type, BigDecimal amount, String description,
            List<String> tags) {
    }
}
