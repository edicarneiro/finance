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

class GetPeriodComparisonReportUseCaseTest {

    private InMemoryTransactionRepository transactionRepository;
    private InMemoryCategoryRepository categoryRepository;
    private GetPeriodComparisonReportUseCase useCase;

    @BeforeEach
    void setUp() {
        transactionRepository = new InMemoryTransactionRepository();
        categoryRepository = new InMemoryCategoryRepository();
        useCase = new GetPeriodComparisonReportUseCase(transactionRepository, categoryRepository);

        categoryRepository.save(Category.create("cat-food", "user-1", "Alimentação", null));
    }

    @Test
    void comparesTwoExplicitPeriodsUsingRealCategoryNames() {
        transactionRepository.save(transaction("tx-a", "cat-food", "200", LocalDate.of(2026, 6, 15)));
        transactionRepository.save(transaction("tx-b", "cat-food", "300", LocalDate.of(2026, 7, 15)));

        GetPeriodComparisonReportUseCase.Output output = useCase.execute(new GetPeriodComparisonReportUseCase.Input(
                "user-1", LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31)));

        assertThat(output.periodA().totalExpense()).isEqualByComparingTo("200");
        assertThat(output.periodB().totalExpense()).isEqualByComparingTo("300");
        assertThat(output.categoryComparisons()).hasSize(1);
        assertThat(output.categoryComparisons().get(0).categoryName()).isEqualTo("Alimentação");
        assertThat(output.categoryComparisons().get(0).delta()).isEqualByComparingTo("100");
    }

    @Test
    void rejectsAnInvertedPeriodA() {
        assertThatThrownBy(() -> useCase.execute(new GetPeriodComparisonReportUseCase.Input(
                        "user-1", LocalDate.of(2026, 6, 30), LocalDate.of(2026, 6, 1), LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31))))
                .isInstanceOf(InvalidReportPeriodException.class);
    }

    private static Transaction transaction(String id, String categoryId, String amount, LocalDate date) {
        return Transaction.create(id, "user-1", "account-1", categoryId, TransactionType.EXPENSE, new BigDecimal(amount), date, null, List.of());
    }
}
