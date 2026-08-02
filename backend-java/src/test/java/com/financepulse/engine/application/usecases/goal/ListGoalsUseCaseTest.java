package com.financepulse.engine.application.usecases.goal;

import static org.assertj.core.api.Assertions.assertThat;

import com.financepulse.engine.application.usecases.account.CreateAccountUseCase;
import com.financepulse.engine.application.usecases.transaction.CreateTransactionUseCase;
import com.financepulse.engine.domain.account.AccountType;
import com.financepulse.engine.domain.category.Category;
import com.financepulse.engine.domain.transaction.TransactionType;
import com.financepulse.engine.testsupport.FixedClock;
import com.financepulse.engine.testsupport.InMemoryAccountRepository;
import com.financepulse.engine.testsupport.InMemoryCategoryRepository;
import com.financepulse.engine.testsupport.InMemoryGoalRepository;
import com.financepulse.engine.testsupport.InMemoryTransactionRepository;
import com.financepulse.engine.testsupport.SequentialIdGenerator;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ListGoalsUseCaseTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 7, 31);

    private InMemoryGoalRepository goalRepository;
    private InMemoryAccountRepository accountRepository;
    private InMemoryCategoryRepository categoryRepository;
    private InMemoryTransactionRepository transactionRepository;
    private CreateGoalUseCase createGoal;
    private ListGoalsUseCase useCase;

    @BeforeEach
    void setUp() {
        goalRepository = new InMemoryGoalRepository();
        accountRepository = new InMemoryAccountRepository();
        categoryRepository = new InMemoryCategoryRepository();
        transactionRepository = new InMemoryTransactionRepository();
        createGoal = new CreateGoalUseCase(
                goalRepository, accountRepository, categoryRepository, new SequentialIdGenerator("goal"), new FixedClock(TODAY));
        useCase = new ListGoalsUseCase(goalRepository, accountRepository, transactionRepository, new FixedClock(TODAY));
    }

    @Test
    void accountBasedGoalProgressReflectsTheAccountsCurrentBalance() {
        String accountId = new CreateAccountUseCase(accountRepository, new SequentialIdGenerator("account"))
                .execute(new CreateAccountUseCase.Input("user-1", AccountType.SAVINGS, "Poupança", "BRL", new BigDecimal("400.00")))
                .accountId();
        createGoal.execute(new CreateGoalUseCase.Input("user-1", "Reserva", new BigDecimal("1000.00"), LocalDate.of(2026, 12, 31), accountId, null, null));

        ListGoalsUseCase.Output result = useCase.execute(new ListGoalsUseCase.Input("user-1"));

        assertThat(result.goals()).hasSize(1);
        assertThat(result.goals().get(0).currentAmount()).isEqualByComparingTo("400.00");
        assertThat(result.goals().get(0).progressPercentage()).isEqualByComparingTo("40.0000");
    }

    @Test
    void categoryBasedGoalProgressSumsTransactionsSinceGoalCreation() {
        Category category = Category.create("category-1", "user-1", "Viagem", null);
        categoryRepository.save(category);
        String accountId = new CreateAccountUseCase(accountRepository, new SequentialIdGenerator("account"))
                .execute(new CreateAccountUseCase.Input("user-1", AccountType.CHECKING, "Conta A", "BRL", BigDecimal.ZERO))
                .accountId();

        createGoal.execute(
                new CreateGoalUseCase.Input("user-1", "Viagem", new BigDecimal("2000.00"), LocalDate.of(2026, 12, 31), null, "category-1", null));

        CreateTransactionUseCase createTransaction =
                new CreateTransactionUseCase(transactionRepository, accountRepository, categoryRepository, new SequentialIdGenerator("tx"));
        createTransaction.execute(new CreateTransactionUseCase.Input(
                "user-1", accountId, "category-1", TransactionType.INCOME, new BigDecimal("500.00"), TODAY, null, List.of()));

        ListGoalsUseCase.Output result = useCase.execute(new ListGoalsUseCase.Input("user-1"));

        assertThat(result.goals().get(0).currentAmount()).isEqualByComparingTo("500.00");
    }

    @Test
    void marksAGoalAsAchievedWhenTargetIsReached() {
        String accountId = new CreateAccountUseCase(accountRepository, new SequentialIdGenerator("account"))
                .execute(new CreateAccountUseCase.Input("user-1", AccountType.SAVINGS, "Poupança", "BRL", new BigDecimal("1000.00")))
                .accountId();
        createGoal.execute(new CreateGoalUseCase.Input("user-1", "Reserva", new BigDecimal("1000.00"), LocalDate.of(2026, 12, 31), accountId, null, null));

        ListGoalsUseCase.Output result = useCase.execute(new ListGoalsUseCase.Input("user-1"));

        assertThat(result.goals().get(0).achieved()).isTrue();
    }

    @Test
    void returnsAnEmptyListWhenTheUserHasNoGoals() {
        ListGoalsUseCase.Output result = useCase.execute(new ListGoalsUseCase.Input("user-without-goals"));

        assertThat(result.goals()).isEmpty();
    }
}
