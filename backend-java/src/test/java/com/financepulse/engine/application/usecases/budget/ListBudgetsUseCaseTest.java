package com.financepulse.engine.application.usecases.budget;

import static org.assertj.core.api.Assertions.assertThat;

import com.financepulse.engine.application.usecases.account.CreateAccountUseCase;
import com.financepulse.engine.application.usecases.transaction.CreateTransactionUseCase;
import com.financepulse.engine.domain.account.AccountType;
import com.financepulse.engine.domain.budget.BudgetPeriodType;
import com.financepulse.engine.domain.category.Category;
import com.financepulse.engine.domain.transaction.Transaction;
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

class ListBudgetsUseCaseTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 7, 31);

    private InMemoryBudgetRepository budgetRepository;
    private InMemoryTransactionRepository transactionRepository;
    private ListBudgetsUseCase useCase;
    private String categoryId;
    private String accountId;

    @BeforeEach
    void setUp() {
        budgetRepository = new InMemoryBudgetRepository();
        InMemoryCategoryRepository categoryRepository = new InMemoryCategoryRepository();
        InMemoryAccountRepository accountRepository = new InMemoryAccountRepository();
        transactionRepository = new InMemoryTransactionRepository();
        useCase = new ListBudgetsUseCase(budgetRepository, transactionRepository, new FixedClock(TODAY));

        Category category = Category.create("category-1", "user-1", "Alimentação", null);
        categoryRepository.save(category);
        categoryId = category.getId();

        accountId = new CreateAccountUseCase(accountRepository, new SequentialIdGenerator("account"))
                .execute(new CreateAccountUseCase.Input("user-1", AccountType.CHECKING, "Conta A", "BRL", BigDecimal.ZERO))
                .accountId();

        new CreateBudgetUseCase(budgetRepository, categoryRepository, new SequentialIdGenerator("budget"))
                .execute(new CreateBudgetUseCase.Input(
                        "user-1", categoryId, new BigDecimal("100.00"), BudgetPeriodType.MONTHLY, null, null, List.of(80, 100)));

        new CreateTransactionUseCase(transactionRepository, accountRepository, categoryRepository, new SequentialIdGenerator("tx"))
                .execute(new CreateTransactionUseCase.Input(
                        "user-1", accountId, categoryId, TransactionType.EXPENSE, new BigDecimal("85.00"), LocalDate.of(2026, 7, 10), null, List.of()));
    }

    @Test
    void listsBudgetsWithCurrentPeriodConsumption() {
        ListBudgetsUseCase.Output result = useCase.execute(new ListBudgetsUseCase.Input("user-1"));

        assertThat(result.budgets()).hasSize(1);
        var view = result.budgets().get(0);
        assertThat(view.consumedAmount()).isEqualByComparingTo("85.00");
        assertThat(view.consumedPercentage()).isEqualByComparingTo("85.0000");
        assertThat(view.thresholdsCrossed()).containsExactly(80);
        assertThat(view.periodStart()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(view.periodEnd()).isEqualTo(LocalDate.of(2026, 7, 31));
    }

    @Test
    void excludesTransactionsFromOtherPeriods() {
        transactionRepository.save(Transaction.create(
                "tx-june", "user-1", accountId, categoryId, TransactionType.EXPENSE, new BigDecimal("500.00"), LocalDate.of(2026, 6, 15), null, List.of()));

        ListBudgetsUseCase.Output result = useCase.execute(new ListBudgetsUseCase.Input("user-1"));

        assertThat(result.budgets().get(0).consumedAmount()).isEqualByComparingTo("85.00");
    }

    @Test
    void returnsAnEmptyListWhenTheUserHasNoBudgets() {
        ListBudgetsUseCase.Output result = useCase.execute(new ListBudgetsUseCase.Input("user-without-budgets"));

        assertThat(result.budgets()).isEmpty();
    }
}
