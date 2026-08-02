package com.financepulse.engine.application.usecases.goal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.financepulse.engine.application.usecases.account.CreateAccountUseCase;
import com.financepulse.engine.domain.account.AccountType;
import com.financepulse.engine.domain.account.errors.AccountNotFoundException;
import com.financepulse.engine.domain.category.Category;
import com.financepulse.engine.domain.category.errors.CategoryNotFoundException;
import com.financepulse.engine.domain.goal.errors.InvalidGoalAssociationException;
import com.financepulse.engine.testsupport.FixedClock;
import com.financepulse.engine.testsupport.InMemoryAccountRepository;
import com.financepulse.engine.testsupport.InMemoryCategoryRepository;
import com.financepulse.engine.testsupport.InMemoryGoalRepository;
import com.financepulse.engine.testsupport.SequentialIdGenerator;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CreateGoalUseCaseTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 7, 31);

    private InMemoryGoalRepository goalRepository;
    private InMemoryAccountRepository accountRepository;
    private InMemoryCategoryRepository categoryRepository;
    private CreateGoalUseCase useCase;
    private String accountId;
    private String categoryId;

    @BeforeEach
    void setUp() {
        goalRepository = new InMemoryGoalRepository();
        accountRepository = new InMemoryAccountRepository();
        categoryRepository = new InMemoryCategoryRepository();
        useCase = new CreateGoalUseCase(goalRepository, accountRepository, categoryRepository, new SequentialIdGenerator("goal"), new FixedClock(TODAY));

        accountId = new CreateAccountUseCase(accountRepository, new SequentialIdGenerator("account"))
                .execute(new CreateAccountUseCase.Input("user-1", AccountType.SAVINGS, "Poupança", "BRL", BigDecimal.ZERO))
                .accountId();
        Category category = Category.create("category-1", "user-1", "Viagem", null);
        categoryRepository.save(category);
        categoryId = category.getId();
    }

    @Test
    void createsAnAccountBasedGoal() {
        CreateGoalUseCase.Output result = useCase.execute(new CreateGoalUseCase.Input(
                "user-1", "Reserva", new BigDecimal("10000.00"), LocalDate.of(2026, 12, 31), accountId, null, null));

        assertThat(result.goalId()).isEqualTo("goal-1");
        assertThat(goalRepository.findByIdAndUserId("goal-1", "user-1")).isPresent();
    }

    @Test
    void createsACategoryBasedGoal() {
        CreateGoalUseCase.Output result = useCase.execute(
                new CreateGoalUseCase.Input("user-1", "Viagem", new BigDecimal("5000.00"), LocalDate.of(2026, 12, 31), null, categoryId, null));

        assertThat(goalRepository.findByIdAndUserId(result.goalId(), "user-1")).isPresent();
    }

    @Test
    void rejectsBothAssociationsProvided() {
        assertThatThrownBy(() -> useCase.execute(new CreateGoalUseCase.Input(
                        "user-1", "Meta", BigDecimal.TEN, LocalDate.of(2026, 12, 31), accountId, categoryId, null)))
                .isInstanceOf(InvalidGoalAssociationException.class);
    }

    @Test
    void rejectsANonExistentAccount() {
        assertThatThrownBy(() -> useCase.execute(
                        new CreateGoalUseCase.Input("user-1", "Meta", BigDecimal.TEN, LocalDate.of(2026, 12, 31), "ghost-account", null, null)))
                .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    void rejectsAnAccountBelongingToAnotherUser() {
        assertThatThrownBy(() -> useCase.execute(
                        new CreateGoalUseCase.Input("another-user", "Meta", BigDecimal.TEN, LocalDate.of(2026, 12, 31), accountId, null, null)))
                .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    void rejectsANonExistentCategory() {
        assertThatThrownBy(() -> useCase.execute(
                        new CreateGoalUseCase.Input("user-1", "Meta", BigDecimal.TEN, LocalDate.of(2026, 12, 31), null, "ghost-category", null)))
                .isInstanceOf(CategoryNotFoundException.class);
    }

    @Test
    void treatsABlankAccountIdAsAbsentSoACategoryGoalIsNotMistakenForAnInvalidAccount() {
        CreateGoalUseCase.Output result = useCase.execute(
                new CreateGoalUseCase.Input("user-1", "Viagem", new BigDecimal("5000.00"), LocalDate.of(2026, 12, 31), "", categoryId, null));

        assertThat(goalRepository.findByIdAndUserId(result.goalId(), "user-1")).isPresent();
    }

    @Test
    void rejectsBothAssociationsBlankTheSameAsBothAbsent() {
        assertThatThrownBy(() -> useCase.execute(
                        new CreateGoalUseCase.Input("user-1", "Meta", BigDecimal.TEN, LocalDate.of(2026, 12, 31), "", "", null)))
                .isInstanceOf(InvalidGoalAssociationException.class);
    }
}
