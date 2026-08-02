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
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UpdateGoalUseCaseTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 7, 31);

    private InMemoryGoalRepository goalRepository;
    private UpdateGoalUseCase useCase;
    private String goalId;

    @BeforeEach
    void setUp() {
        goalRepository = new InMemoryGoalRepository();
        InMemoryAccountRepository accountRepository = new InMemoryAccountRepository();
        InMemoryCategoryRepository categoryRepository = new InMemoryCategoryRepository();
        useCase = new UpdateGoalUseCase(goalRepository, new FixedClock(TODAY));

        String accountId = new CreateAccountUseCase(accountRepository, new SequentialIdGenerator("account"))
                .execute(new CreateAccountUseCase.Input("user-1", AccountType.SAVINGS, "Poupança", "BRL", BigDecimal.ZERO))
                .accountId();

        goalId = new CreateGoalUseCase(goalRepository, accountRepository, categoryRepository, new SequentialIdGenerator("goal"), new FixedClock(TODAY))
                .execute(new CreateGoalUseCase.Input("user-1", "Reserva", new BigDecimal("1000.00"), LocalDate.of(2026, 12, 31), accountId, null, null))
                .goalId();
    }

    @Test
    void updatesNameTargetDeadlineAndThresholds() {
        useCase.execute(new UpdateGoalUseCase.Input(
                "user-1", goalId, "Reserva de emergência", new BigDecimal("2000.00"), LocalDate.of(2027, 6, 30), List.of(90)));

        var updated = goalRepository.findByIdAndUserId(goalId, "user-1").orElseThrow();
        assertThat(updated.getName()).isEqualTo("Reserva de emergência");
        assertThat(updated.getTargetAmount()).isEqualByComparingTo("2000.00");
        assertThat(updated.getDeadline()).isEqualTo(LocalDate.of(2027, 6, 30));
        assertThat(updated.getProgressAlertThresholds()).containsExactly(90);
    }

    @Test
    void rejectsUpdatingANonExistentGoal() {
        assertThatThrownBy(() -> useCase.execute(
                        new UpdateGoalUseCase.Input("user-1", "ghost-goal", "Meta", BigDecimal.TEN, LocalDate.of(2027, 1, 1), null)))
                .isInstanceOf(GoalNotFoundException.class);
    }

    @Test
    void rejectsUpdatingAnotherUsersGoalWithTheSameErrorAsNotFound() {
        assertThatThrownBy(() -> useCase.execute(
                        new UpdateGoalUseCase.Input("another-user", goalId, "Meta", BigDecimal.TEN, LocalDate.of(2027, 1, 1), null)))
                .isInstanceOf(GoalNotFoundException.class);
    }
}
