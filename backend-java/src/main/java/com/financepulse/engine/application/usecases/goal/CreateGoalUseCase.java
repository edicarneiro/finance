package com.financepulse.engine.application.usecases.goal;

import com.financepulse.engine.application.ports.AccountRepository;
import com.financepulse.engine.application.ports.CategoryRepository;
import com.financepulse.engine.application.ports.Clock;
import com.financepulse.engine.application.ports.GoalRepository;
import com.financepulse.engine.application.ports.IdGenerator;
import com.financepulse.engine.domain.account.errors.AccountNotFoundException;
import com.financepulse.engine.domain.category.errors.CategoryNotFoundException;
import com.financepulse.engine.domain.goal.Goal;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class CreateGoalUseCase {

    private final GoalRepository goalRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final IdGenerator idGenerator;
    private final Clock clock;

    public CreateGoalUseCase(
            GoalRepository goalRepository,
            AccountRepository accountRepository,
            CategoryRepository categoryRepository,
            IdGenerator idGenerator,
            Clock clock) {
        this.goalRepository = goalRepository;
        this.accountRepository = accountRepository;
        this.categoryRepository = categoryRepository;
        this.idGenerator = idGenerator;
        this.clock = clock;
    }

    public Output execute(Input input) {
        String accountId = blankToNull(input.accountId());
        String categoryId = blankToNull(input.categoryId());

        if (accountId != null) {
            accountRepository.findByIdAndUserId(accountId, input.userId()).orElseThrow(AccountNotFoundException::new);
        }
        if (categoryId != null) {
            categoryRepository.findByIdAndUserId(categoryId, input.userId()).orElseThrow(CategoryNotFoundException::new);
        }

        Goal goal = Goal.create(
                idGenerator.generate(),
                input.userId(),
                input.name(),
                input.targetAmount(),
                input.deadline(),
                accountId,
                categoryId,
                input.progressAlertThresholds(),
                clock.today());

        goalRepository.save(goal);

        return new Output(goal.getId());
    }

    /** Alinha com GoalPolicy.assertValidAssociation, que trata string em branco como ausente. */
    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    public record Input(
            String userId,
            String name,
            BigDecimal targetAmount,
            LocalDate deadline,
            String accountId,
            String categoryId,
            List<Integer> progressAlertThresholds) {
    }

    public record Output(String goalId) {
    }
}
