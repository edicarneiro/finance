package com.financepulse.engine.application.usecases.budget;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.financepulse.engine.application.usecases.account.CreateAccountUseCase;
import com.financepulse.engine.application.usecases.transaction.CreateTransactionUseCase;
import com.financepulse.engine.domain.account.AccountType;
import com.financepulse.engine.domain.budget.BudgetPeriodType;
import com.financepulse.engine.domain.budget.errors.BudgetNotFoundException;
import com.financepulse.engine.domain.category.Category;
import com.financepulse.engine.domain.transaction.TransactionType;
import com.financepulse.engine.testsupport.FixedClock;
import com.financepulse.engine.testsupport.InMemoryAccountRepository;
import com.financepulse.engine.testsupport.InMemoryBudgetRepository;
import com.financepulse.engine.testsupport.InMemoryCategoryRepository;
import com.financepulse.engine.testsupport.InMemoryTransactionRepository;
import com.financepulse.engine.testsupport.SequentialIdGenerator;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GetBudgetHistoryUseCaseTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 7, 31);

    private InMemoryBudgetRepository budgetRepository;
    private GetBudgetHistoryUseCase useCase;
    private String budgetId;

    @BeforeEach
    void setUp() {
        budgetRepository = new InMemoryBudgetRepository();
        InMemoryCategoryRepository categoryRepository = new InMemoryCategoryRepository();
        InMemoryAccountRepository accountRepository = new InMemoryAccountRepository();
        InMemoryTransactionRepository transactionRepository = new InMemoryTransactionRepository();
        useCase = new GetBudgetHistoryUseCase(budgetRepository, transactionRepository, new FixedClock(TODAY));

        Category category = Category.create("category-1", "user-1", "Alimentação", null);
        categoryRepository.save(category);

        String accountId = new CreateAccountUseCase(accountRepository, new SequentialIdGenerator("account"))
                .execute(new CreateAccountUseCase.Input("user-1", AccountType.CHECKING, "Conta A", "BRL", BigDecimal.ZERO))
                .accountId();

        budgetId = new CreateBudgetUseCase(budgetRepository, categoryRepository, new SequentialIdGenerator("budget"))
                .execute(new CreateBudgetUseCase.Input(
                        "user-1", category.getId(), new BigDecimal("100.00"), BudgetPeriodType.MONTHLY, null, null, null))
                .budgetId();

        CreateTransactionUseCase createTransaction =
                new CreateTransactionUseCase(transactionRepository, accountRepository, categoryRepository, new SequentialIdGenerator("tx"));
        createTransaction.execute(new CreateTransactionUseCase.Input(
                "user-1", accountId, category.getId(), TransactionType.EXPENSE, new BigDecimal("60.00"), LocalDate.of(2026, 6, 10), null, List.of()));
        createTransaction.execute(new CreateTransactionUseCase.Input(
                "user-1", accountId, category.getId(), TransactionType.EXPENSE, new BigDecimal("40.00"), LocalDate.of(2026, 5, 5), null, List.of()));
    }

    @Test
    void returnsConsumptionForEachOfThePreviousPeriods() {
        GetBudgetHistoryUseCase.Output result = useCase.execute(new GetBudgetHistoryUseCase.Input("user-1", budgetId, 3));

        assertThat(result.periods()).hasSize(3);
        assertThat(result.periods().get(0).periodStart()).isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(result.periods().get(0).consumedAmount()).isEqualByComparingTo("60.00");
        assertThat(result.periods().get(1).periodStart()).isEqualTo(LocalDate.of(2026, 5, 1));
        assertThat(result.periods().get(1).consumedAmount()).isEqualByComparingTo("40.00");
        assertThat(result.periods().get(2).consumedAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void usesTheDefaultPeriodCountWhenNotSpecified() {
        GetBudgetHistoryUseCase.Output result = useCase.execute(new GetBudgetHistoryUseCase.Input("user-1", budgetId));

        assertThat(result.periods()).hasSize(GetBudgetHistoryUseCase.DEFAULT_PERIOD_COUNT);
    }

    @Test
    void rejectsANonExistentBudget() {
        assertThatThrownBy(() -> useCase.execute(new GetBudgetHistoryUseCase.Input("user-1", "ghost-budget", 3)))
                .isInstanceOf(BudgetNotFoundException.class);
    }

    @Test
    void rejectsAnotherUsersBudgetWithTheSameErrorAsNotFound() {
        assertThatThrownBy(() -> useCase.execute(new GetBudgetHistoryUseCase.Input("another-user", budgetId, 3)))
                .isInstanceOf(BudgetNotFoundException.class);
    }
}
