package com.financepulse.engine.application.usecases.goal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.financepulse.engine.application.usecases.account.CreateAccountUseCase;
import com.financepulse.engine.domain.account.AccountType;
import com.financepulse.engine.domain.goal.errors.GoalNotFoundException;
import com.financepulse.engine.testsupport.FixedClock;
import com.financepulse.engine.testsupport.InMemoryAccountRepository;
import com.financepulse.engine.testsupport.InMemoryCategoryRepository;
import com.financepulse.engine.testsupport.InMemoryGoalRepository;
import com.financepulse.engine.testsupport.SequentialIdGenerator;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DeleteGoalUseCaseTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 7, 31);

    private InMemoryGoalRepository goalRepository;
    private DeleteGoalUseCase useCase;
    private String goalId;

    @BeforeEach
    void setUp() {
        goalRepository = new InMemoryGoalRepository();
        InMemoryAccountRepository accountRepository = new InMemoryAccountRepository();
        InMemoryCategoryRepository categoryRepository = new InMemoryCategoryRepository();
        useCase = new DeleteGoalUseCase(goalRepository);

        String accountId = new CreateAccountUseCase(accountRepository, new SequentialIdGenerator("account"))
                .execute(new CreateAccountUseCase.Input("user-1", AccountType.SAVINGS, "Poupança", "BRL", BigDecimal.ZERO))
                .accountId();

        goalId = new CreateGoalUseCase(goalRepository, accountRepository, categoryRepository, new SequentialIdGenerator("goal"), new FixedClock(TODAY))
                .execute(new CreateGoalUseCase.Input("user-1", "Reserva", new BigDecimal("1000.00"), LocalDate.of(2026, 12, 31), accountId, null, null))
                .goalId();
    }

    @Test
    void deletesAnOwnedGoal() {
        useCase.execute(new DeleteGoalUseCase.Input("user-1", goalId));

        assertThat(goalRepository.findByIdAndUserId(goalId, "user-1")).isEmpty();
    }

    @Test
    void rejectsDeletingANonExistentGoal() {
        assertThatThrownBy(() -> useCase.execute(new DeleteGoalUseCase.Input("user-1", "ghost-goal"))).isInstanceOf(GoalNotFoundException.class);
    }

    @Test
    void rejectsDeletingAnotherUsersGoalWithTheSameErrorAsNotFound() {
        assertThatThrownBy(() -> useCase.execute(new DeleteGoalUseCase.Input("another-user", goalId)))
                .isInstanceOf(GoalNotFoundException.class);

        assertThat(goalRepository.findByIdAndUserId(goalId, "user-1")).isPresent();
    }
}
