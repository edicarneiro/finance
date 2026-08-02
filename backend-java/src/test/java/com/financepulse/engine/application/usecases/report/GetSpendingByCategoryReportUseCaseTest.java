package com.financepulse.engine.application.usecases.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.financepulse.engine.domain.category.Category;
import com.financepulse.engine.domain.report.errors.InvalidReportPeriodException;
import com.financepulse.engine.domain.transaction.Transaction;
import com.financepulse.engine.domain.transaction.TransactionType;
import com.financepulse.engine.testsupport.InMemoryCategoryRepository;
import com.financepulse.engine.testsupport.InMemoryTransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GetSpendingByCategoryReportUseCaseTest {

    private InMemoryTransactionRepository transactionRepository;
    private InMemoryCategoryRepository categoryRepository;
    private GetSpendingByCategoryReportUseCase useCase;

    @BeforeEach
    void setUp() {
        transactionRepository = new InMemoryTransactionRepository();
        categoryRepository = new InMemoryCategoryRepository();
        useCase = new GetSpendingByCategoryReportUseCase(transactionRepository, categoryRepository);

        categoryRepository.save(Category.create("cat-food", "user-1", "Alimentação", null));
    }

    @Test
    void reportsExpensesGroupedByCategoryWithinTheExplicitPeriod() {
        transactionRepository.save(transaction(TransactionType.EXPENSE, "cat-food", "200", LocalDate.of(2026, 7, 15)));
        transactionRepository.save(transaction(TransactionType.EXPENSE, "cat-food", "50", LocalDate.of(2026, 6, 1)));

        GetSpendingByCategoryReportUseCase.Output output =
                useCase.execute(new GetSpendingByCategoryReportUseCase.Input("user-1", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31)));

        assertThat(output.totalExpense()).isEqualByComparingTo("200");
        assertThat(output.categories()).hasSize(1);
        assertThat(output.categories().get(0).categoryName()).isEqualTo("Alimentação");
        assertThat(output.categories().get(0).percentage()).isEqualByComparingTo("100");
    }

    @Test
    void rejectsAnInvertedPeriod() {
        assertThatThrownBy(() -> useCase.execute(
                        new GetSpendingByCategoryReportUseCase.Input("user-1", LocalDate.of(2026, 7, 31), LocalDate.of(2026, 7, 1))))
                .isInstanceOf(InvalidReportPeriodException.class);
    }

    @Test
    void doesNotMixTransactionsFromAnotherUser() {
        Transaction otherUsersTransaction = Transaction.create(
                "tx-other", "another-user", "account-1", "cat-food", TransactionType.EXPENSE, new BigDecimal("999"), LocalDate.of(2026, 7, 10), null,
                List.of());
        transactionRepository.save(otherUsersTransaction);

        GetSpendingByCategoryReportUseCase.Output output =
                useCase.execute(new GetSpendingByCategoryReportUseCase.Input("user-1", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31)));

        assertThat(output.totalExpense()).isEqualByComparingTo("0");
    }

    private static Transaction transaction(TransactionType type, String categoryId, String amount, LocalDate date) {
        return Transaction.create(
                "tx-" + categoryId + "-" + amount + "-" + date, "user-1", "account-1", categoryId, type, new BigDecimal(amount), date, null, List.of());
    }
}
