package com.financepulse.engine.application.usecases.goal;

import com.financepulse.engine.application.ports.AccountRepository;
import com.financepulse.engine.application.ports.Clock;
import com.financepulse.engine.application.ports.GoalRepository;
import com.financepulse.engine.application.ports.TransactionRepository;
import com.financepulse.engine.application.services.AccountBalanceCalculator;
import com.financepulse.engine.application.services.GoalProgressCalculator;
import com.financepulse.engine.application.services.GoalProgressCalculator.Progress;
import com.financepulse.engine.domain.account.errors.AccountNotFoundException;
import com.financepulse.engine.domain.goal.Goal;
import com.financepulse.engine.domain.transaction.Transaction;
import com.financepulse.engine.domain.transaction.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

/** RF-031 (progresso em tempo real) + RF-032 (sinal de limiares/conclusão, ver ADR-0019). */
public class ListGoalsUseCase {

    private final GoalRepository goalRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final Clock clock;

    public ListGoalsUseCase(
            GoalRepository goalRepository, AccountRepository accountRepository, TransactionRepository transactionRepository, Clock clock) {
        this.goalRepository = goalRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.clock = clock;
    }

    public Output execute(Input input) {
        LocalDate today = clock.today();

        List<GoalView> views = goalRepository.findAllByUserId(input.userId()).stream()
                .map(goal -> toView(goal, today))
                .toList();

        return new Output(views);
    }

    private GoalView toView(Goal goal, LocalDate today) {
        BigDecimal currentAmount = goal.isAccountBased() ? accountBasedAmount(goal) : categoryBasedAmount(goal);
        Progress progress = GoalProgressCalculator.calculate(goal, currentAmount, today);

        return new GoalView(
                goal.getId(),
                goal.getName(),
                goal.getTargetAmount(),
                goal.getDeadline(),
                goal.getAccountId().orElse(null),
                goal.getCategoryId().orElse(null),
                goal.getProgressAlertThresholds(),
                progress.currentAmount(),
                progress.percentage(),
                progress.thresholdsCrossed(),
                progress.achieved(),
                progress.overdue());
    }

    private BigDecimal accountBasedAmount(Goal goal) {
        String accountId = goal.getAccountId().orElseThrow();
        var account = accountRepository.findByIdAndUserId(accountId, goal.getUserId()).orElseThrow(AccountNotFoundException::new);
        List<Transaction> transactions = transactionRepository.findAllByAccountIdAndUserId(accountId, goal.getUserId());

        return AccountBalanceCalculator.currentBalance(account, transactions);
    }

    private BigDecimal categoryBasedAmount(Goal goal) {
        String categoryId = goal.getCategoryId().orElseThrow();
        LocalDate since = goal.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate();

        return transactionRepository.findAllByCategoryIdAndUserId(categoryId, goal.getUserId()).stream()
                .filter(transaction -> !transaction.getDate().isBefore(since))
                .map(transaction -> transaction.getType() == TransactionType.INCOME ? transaction.getAmount() : transaction.getAmount().negate())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public record Input(String userId) {
    }

    public record Output(List<GoalView> goals) {
    }

    public record GoalView(
            String id,
            String name,
            BigDecimal targetAmount,
            LocalDate deadline,
            String accountId,
            String categoryId,
            List<Integer> progressAlertThresholds,
            BigDecimal currentAmount,
            BigDecimal progressPercentage,
            List<Integer> thresholdsCrossed,
            boolean achieved,
            boolean overdue) {
    }
}
