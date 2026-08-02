package com.financepulse.engine.application.usecases.user;

import com.financepulse.engine.application.ports.AccountRepository;
import com.financepulse.engine.application.ports.BudgetRepository;
import com.financepulse.engine.application.ports.CategoryRepository;
import com.financepulse.engine.application.ports.ConsentRepository;
import com.financepulse.engine.application.ports.GoalRepository;
import com.financepulse.engine.application.ports.NotificationPreferenceRepository;
import com.financepulse.engine.application.ports.NotificationRepository;
import com.financepulse.engine.application.ports.PulseScoreRepository;
import com.financepulse.engine.application.ports.TransactionRepository;
import com.financepulse.engine.application.ports.UserRepository;
import com.financepulse.engine.domain.account.Account;
import com.financepulse.engine.domain.account.AccountType;
import com.financepulse.engine.domain.budget.Budget;
import com.financepulse.engine.domain.budget.BudgetPeriodType;
import com.financepulse.engine.domain.category.Category;
import com.financepulse.engine.domain.goal.Goal;
import com.financepulse.engine.domain.notification.AlertType;
import com.financepulse.engine.domain.notification.Notification;
import com.financepulse.engine.domain.notification.NotificationChannel;
import com.financepulse.engine.domain.notification.NotificationPreference;
import com.financepulse.engine.domain.pulsescore.PulseScoreSnapshot;
import com.financepulse.engine.domain.transaction.Transaction;
import com.financepulse.engine.domain.transaction.TransactionType;
import com.financepulse.engine.domain.user.User;
import com.financepulse.engine.domain.user.errors.UserNotFoundException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * RF-044 (ver ADR-0023): agrega todos os dados pessoais e financeiros do
 * usuário já armazenados pelo backend Java em um único documento — nunca
 * inclui {@code passwordHash}. Sem cálculo, apenas leitura e montagem;
 * nenhum valor é derivado além do que os próprios repositórios já retornam.
 */
public class ExportUserDataUseCase {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final BudgetRepository budgetRepository;
    private final GoalRepository goalRepository;
    private final PulseScoreRepository pulseScoreRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceRepository notificationPreferenceRepository;
    private final ConsentRepository consentRepository;

    public ExportUserDataUseCase(
            UserRepository userRepository,
            AccountRepository accountRepository,
            TransactionRepository transactionRepository,
            CategoryRepository categoryRepository,
            BudgetRepository budgetRepository,
            GoalRepository goalRepository,
            PulseScoreRepository pulseScoreRepository,
            NotificationRepository notificationRepository,
            NotificationPreferenceRepository notificationPreferenceRepository,
            ConsentRepository consentRepository) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
        this.budgetRepository = budgetRepository;
        this.goalRepository = goalRepository;
        this.pulseScoreRepository = pulseScoreRepository;
        this.notificationRepository = notificationRepository;
        this.notificationPreferenceRepository = notificationPreferenceRepository;
        this.consentRepository = consentRepository;
    }

    public Output execute(Input input) {
        String userId = input.userId();
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);

        return new Output(
                toProfile(user),
                accountRepository.findAllByUserId(userId).stream().map(ExportUserDataUseCase::toAccount).toList(),
                transactionRepository.findAllByUserId(userId).stream().map(ExportUserDataUseCase::toTransaction).toList(),
                categoryRepository.findAllByUserId(userId).stream().map(ExportUserDataUseCase::toCategory).toList(),
                budgetRepository.findAllByUserId(userId).stream().map(ExportUserDataUseCase::toBudget).toList(),
                goalRepository.findAllByUserId(userId).stream().map(ExportUserDataUseCase::toGoal).toList(),
                pulseScoreRepository.findAllByUserId(userId).stream().map(ExportUserDataUseCase::toPulseScore).toList(),
                notificationRepository.findAllByUserId(userId).stream().map(ExportUserDataUseCase::toNotification).toList(),
                notificationPreferenceRepository.findAllByUserId(userId).stream().map(ExportUserDataUseCase::toPreference).toList(),
                consentRepository.findAllByUserId(userId).stream()
                        .map(record -> new ConsentExport(record.getVersion(), record.getAcceptedAt()))
                        .toList());
    }

    private static ProfileExport toProfile(User user) {
        return new ProfileExport(user.getId(), user.getEmail().toString(), user.getName(), user.getCreatedAt(), user.getDeletedAt().orElse(null));
    }

    private static AccountExport toAccount(Account account) {
        return new AccountExport(
                account.getId(), account.getType(), account.getName(), account.getCurrency().toString(), account.getBalance(), account.isArchived(),
                account.getCreatedAt());
    }

    private static TransactionExport toTransaction(Transaction transaction) {
        return new TransactionExport(
                transaction.getId(), transaction.getAccountId(), transaction.getCategoryId(), transaction.getType(), transaction.getAmount(),
                transaction.getDate(), transaction.getDescription(), transaction.getTags(), transaction.getCreatedAt());
    }

    private static CategoryExport toCategory(Category category) {
        return new CategoryExport(category.getId(), category.getName(), category.getParentCategoryId().orElse(null), category.getCreatedAt());
    }

    private static BudgetExport toBudget(Budget budget) {
        return new BudgetExport(
                budget.getId(), budget.getCategoryId(), budget.getLimitAmount(), budget.getPeriodType(), budget.getCustomPeriodStart().orElse(null),
                budget.getCustomPeriodEnd().orElse(null), budget.getAlertThresholds(), budget.getCreatedAt());
    }

    private static GoalExport toGoal(Goal goal) {
        return new GoalExport(
                goal.getId(), goal.getName(), goal.getTargetAmount(), goal.getDeadline(), goal.getAccountId().orElse(null),
                goal.getCategoryId().orElse(null), goal.getProgressAlertThresholds(), goal.getCreatedAt());
    }

    private static PulseScoreExport toPulseScore(PulseScoreSnapshot snapshot) {
        return new PulseScoreExport(
                snapshot.getScoreDate(),
                snapshot.getOverallScore(),
                snapshot.getFormulaVersion(),
                snapshot.getBudgetConsistencyScore().orElse(null),
                snapshot.getSavingsRateScore().orElse(null),
                snapshot.getSpendingDiversificationScore().orElse(null),
                snapshot.getBalanceTrendScore());
    }

    private static NotificationExport toNotification(Notification notification) {
        return new NotificationExport(
                notification.getId(), notification.getAlertType(), notification.getMessage(), notification.getDeliveredChannels(),
                notification.isRead(), notification.getCreatedAt());
    }

    private static NotificationPreferenceExport toPreference(NotificationPreference preference) {
        return new NotificationPreferenceExport(preference.getAlertType(), preference.getChannel(), preference.isEnabled());
    }

    public record Input(String userId) {
    }

    public record Output(
            ProfileExport profile,
            List<AccountExport> accounts,
            List<TransactionExport> transactions,
            List<CategoryExport> categories,
            List<BudgetExport> budgets,
            List<GoalExport> goals,
            List<PulseScoreExport> pulseScoreHistory,
            List<NotificationExport> notifications,
            List<NotificationPreferenceExport> notificationPreferences,
            List<ConsentExport> consentHistory) {
    }

    public record ProfileExport(String id, String email, String name, Instant createdAt, Instant deletedAt) {
    }

    public record AccountExport(
            String id, AccountType type, String name, String currency, BigDecimal balance, boolean archived, Instant createdAt) {
    }

    public record TransactionExport(
            String id, String accountId, String categoryId, TransactionType type, BigDecimal amount, LocalDate date, String description,
            List<String> tags, Instant createdAt) {
    }

    public record CategoryExport(String id, String name, String parentCategoryId, Instant createdAt) {
    }

    public record BudgetExport(
            String id, String categoryId, BigDecimal limitAmount, BudgetPeriodType periodType, LocalDate customPeriodStart,
            LocalDate customPeriodEnd, List<Integer> alertThresholds, Instant createdAt) {
    }

    public record GoalExport(
            String id, String name, BigDecimal targetAmount, LocalDate deadline, String accountId, String categoryId,
            List<Integer> progressAlertThresholds, Instant createdAt) {
    }

    public record PulseScoreExport(
            LocalDate scoreDate, BigDecimal overallScore, String formulaVersion, BigDecimal budgetConsistencyScore, BigDecimal savingsRateScore,
            BigDecimal spendingDiversificationScore, BigDecimal balanceTrendScore) {
    }

    public record NotificationExport(
            String id, AlertType alertType, String message, Set<NotificationChannel> deliveredChannels, boolean read, Instant createdAt) {
    }

    public record NotificationPreferenceExport(AlertType alertType, NotificationChannel channel, boolean enabled) {
    }

    public record ConsentExport(String version, Instant acceptedAt) {
    }
}
