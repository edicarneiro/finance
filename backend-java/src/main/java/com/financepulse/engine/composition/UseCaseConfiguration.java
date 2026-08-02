package com.financepulse.engine.composition;

import com.financepulse.engine.application.ports.AccountRepository;
import com.financepulse.engine.application.ports.AlertEmailNotifier;
import com.financepulse.engine.application.ports.AuditLogRepository;
import com.financepulse.engine.application.ports.BudgetRepository;
import com.financepulse.engine.application.ports.CategoryRepository;
import com.financepulse.engine.application.ports.Clock;
import com.financepulse.engine.application.ports.ConsentRepository;
import com.financepulse.engine.application.ports.GoalRepository;
import com.financepulse.engine.application.ports.IdGenerator;
import com.financepulse.engine.application.ports.NotificationPreferenceRepository;
import com.financepulse.engine.application.ports.NotificationRepository;
import com.financepulse.engine.application.ports.PasswordHasher;
import com.financepulse.engine.application.ports.PulseScoreRepository;
import com.financepulse.engine.application.ports.TokenService;
import com.financepulse.engine.application.ports.TransactionRepository;
import com.financepulse.engine.application.ports.UserRepository;
import com.financepulse.engine.application.usecases.AuthenticateUserUseCase;
import com.financepulse.engine.application.usecases.RegisterUserUseCase;
import com.financepulse.engine.application.usecases.account.ArchiveAccountUseCase;
import com.financepulse.engine.application.usecases.backoffice.GetAuditLogUseCase;
import com.financepulse.engine.application.usecases.backoffice.GetUserForSupportUseCase;
import com.financepulse.engine.application.usecases.backoffice.ReactivateAccountUseCase;
import com.financepulse.engine.application.usecases.backoffice.SuspendAccountUseCase;
import com.financepulse.engine.application.usecases.account.CreateAccountUseCase;
import com.financepulse.engine.application.usecases.account.GetConsolidatedBalanceUseCase;
import com.financepulse.engine.application.usecases.account.ListAccountsUseCase;
import com.financepulse.engine.application.usecases.account.UpdateAccountUseCase;
import com.financepulse.engine.application.usecases.budget.CreateBudgetUseCase;
import com.financepulse.engine.application.usecases.budget.DeleteBudgetUseCase;
import com.financepulse.engine.application.usecases.budget.GetBudgetHistoryUseCase;
import com.financepulse.engine.application.usecases.budget.ListBudgetsUseCase;
import com.financepulse.engine.application.usecases.budget.UpdateBudgetUseCase;
import com.financepulse.engine.application.usecases.category.CreateCategoryUseCase;
import com.financepulse.engine.application.usecases.category.DeleteCategoryUseCase;
import com.financepulse.engine.application.usecases.category.ListCategoriesUseCase;
import com.financepulse.engine.application.usecases.category.UpdateCategoryUseCase;
import com.financepulse.engine.application.usecases.dashboard.GetDashboardUseCase;
import com.financepulse.engine.application.usecases.dashboard.GetPulseScoreHistoryUseCase;
import com.financepulse.engine.application.usecases.goal.CreateGoalUseCase;
import com.financepulse.engine.application.usecases.goal.DeleteGoalUseCase;
import com.financepulse.engine.application.usecases.goal.ListGoalsUseCase;
import com.financepulse.engine.application.usecases.goal.UpdateGoalUseCase;
import com.financepulse.engine.application.usecases.notification.CheckNotificationsUseCase;
import com.financepulse.engine.application.usecases.notification.GetNotificationPreferencesUseCase;
import com.financepulse.engine.application.usecases.notification.ListNotificationsUseCase;
import com.financepulse.engine.application.usecases.notification.MarkNotificationReadUseCase;
import com.financepulse.engine.application.usecases.notification.UpdateNotificationPreferencesUseCase;
import com.financepulse.engine.application.usecases.report.GetPeriodComparisonReportUseCase;
import com.financepulse.engine.application.usecases.report.GetSpendingByCategoryReportUseCase;
import com.financepulse.engine.application.usecases.report.GetTransactionsForPeriodUseCase;
import com.financepulse.engine.application.usecases.transaction.CreateTransactionUseCase;
import com.financepulse.engine.application.usecases.user.DeleteAccountUseCase;
import com.financepulse.engine.application.usecases.user.ExportUserDataUseCase;
import com.financepulse.engine.application.usecases.user.ListConsentHistoryUseCase;
import com.financepulse.engine.application.usecases.user.RecordConsentUseCase;
import com.financepulse.engine.application.usecases.transaction.DeleteTransactionUseCase;
import com.financepulse.engine.application.usecases.transaction.ListTransactionsUseCase;
import com.financepulse.engine.application.usecases.transaction.UpdateTransactionUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Raiz de composição da camada de aplicação (equivalente ao container.ts do
 * backend TypeScript, revisado por ADR-0013): os casos de uso permanecem
 * classes Java puras, sem anotação de framework (regra de dependência da
 * Arquitetura Hexagonal), então precisam ser instanciados explicitamente
 * aqui em vez de descobertos via @Component/@Service.
 */
@Configuration
public class UseCaseConfiguration {

    @Bean
    public RegisterUserUseCase registerUserUseCase(
            UserRepository userRepository, PasswordHasher passwordHasher, IdGenerator idGenerator) {
        return new RegisterUserUseCase(userRepository, passwordHasher, idGenerator);
    }

    @Bean
    public AuthenticateUserUseCase authenticateUserUseCase(
            UserRepository userRepository, PasswordHasher passwordHasher, TokenService tokenService) {
        return new AuthenticateUserUseCase(userRepository, passwordHasher, tokenService);
    }

    @Bean
    public CreateAccountUseCase createAccountUseCase(AccountRepository accountRepository, IdGenerator idGenerator) {
        return new CreateAccountUseCase(accountRepository, idGenerator);
    }

    @Bean
    public UpdateAccountUseCase updateAccountUseCase(AccountRepository accountRepository) {
        return new UpdateAccountUseCase(accountRepository);
    }

    @Bean
    public ArchiveAccountUseCase archiveAccountUseCase(AccountRepository accountRepository) {
        return new ArchiveAccountUseCase(accountRepository);
    }

    @Bean
    public ListAccountsUseCase listAccountsUseCase(AccountRepository accountRepository, TransactionRepository transactionRepository) {
        return new ListAccountsUseCase(accountRepository, transactionRepository);
    }

    @Bean
    public GetConsolidatedBalanceUseCase getConsolidatedBalanceUseCase(
            AccountRepository accountRepository, TransactionRepository transactionRepository) {
        return new GetConsolidatedBalanceUseCase(accountRepository, transactionRepository);
    }

    @Bean
    public ListCategoriesUseCase listCategoriesUseCase(CategoryRepository categoryRepository, IdGenerator idGenerator) {
        return new ListCategoriesUseCase(categoryRepository, idGenerator);
    }

    @Bean
    public CreateCategoryUseCase createCategoryUseCase(CategoryRepository categoryRepository, IdGenerator idGenerator) {
        return new CreateCategoryUseCase(categoryRepository, idGenerator);
    }

    @Bean
    public UpdateCategoryUseCase updateCategoryUseCase(CategoryRepository categoryRepository) {
        return new UpdateCategoryUseCase(categoryRepository);
    }

    @Bean
    public DeleteCategoryUseCase deleteCategoryUseCase(CategoryRepository categoryRepository, TransactionRepository transactionRepository) {
        return new DeleteCategoryUseCase(categoryRepository, transactionRepository);
    }

    @Bean
    public CreateTransactionUseCase createTransactionUseCase(
            TransactionRepository transactionRepository,
            AccountRepository accountRepository,
            CategoryRepository categoryRepository,
            IdGenerator idGenerator) {
        return new CreateTransactionUseCase(transactionRepository, accountRepository, categoryRepository, idGenerator);
    }

    @Bean
    public UpdateTransactionUseCase updateTransactionUseCase(
            TransactionRepository transactionRepository, AccountRepository accountRepository, CategoryRepository categoryRepository) {
        return new UpdateTransactionUseCase(transactionRepository, accountRepository, categoryRepository);
    }

    @Bean
    public DeleteTransactionUseCase deleteTransactionUseCase(TransactionRepository transactionRepository) {
        return new DeleteTransactionUseCase(transactionRepository);
    }

    @Bean
    public ListTransactionsUseCase listTransactionsUseCase(
            TransactionRepository transactionRepository, AccountRepository accountRepository) {
        return new ListTransactionsUseCase(transactionRepository, accountRepository);
    }

    @Bean
    public CreateBudgetUseCase createBudgetUseCase(BudgetRepository budgetRepository, CategoryRepository categoryRepository, IdGenerator idGenerator) {
        return new CreateBudgetUseCase(budgetRepository, categoryRepository, idGenerator);
    }

    @Bean
    public UpdateBudgetUseCase updateBudgetUseCase(BudgetRepository budgetRepository) {
        return new UpdateBudgetUseCase(budgetRepository);
    }

    @Bean
    public DeleteBudgetUseCase deleteBudgetUseCase(BudgetRepository budgetRepository) {
        return new DeleteBudgetUseCase(budgetRepository);
    }

    @Bean
    public ListBudgetsUseCase listBudgetsUseCase(BudgetRepository budgetRepository, TransactionRepository transactionRepository, Clock clock) {
        return new ListBudgetsUseCase(budgetRepository, transactionRepository, clock);
    }

    @Bean
    public GetBudgetHistoryUseCase getBudgetHistoryUseCase(
            BudgetRepository budgetRepository, TransactionRepository transactionRepository, Clock clock) {
        return new GetBudgetHistoryUseCase(budgetRepository, transactionRepository, clock);
    }

    @Bean
    public CreateGoalUseCase createGoalUseCase(
            GoalRepository goalRepository,
            AccountRepository accountRepository,
            CategoryRepository categoryRepository,
            IdGenerator idGenerator,
            Clock clock) {
        return new CreateGoalUseCase(goalRepository, accountRepository, categoryRepository, idGenerator, clock);
    }

    @Bean
    public UpdateGoalUseCase updateGoalUseCase(GoalRepository goalRepository, Clock clock) {
        return new UpdateGoalUseCase(goalRepository, clock);
    }

    @Bean
    public DeleteGoalUseCase deleteGoalUseCase(GoalRepository goalRepository) {
        return new DeleteGoalUseCase(goalRepository);
    }

    @Bean
    public ListGoalsUseCase listGoalsUseCase(
            GoalRepository goalRepository, AccountRepository accountRepository, TransactionRepository transactionRepository, Clock clock) {
        return new ListGoalsUseCase(goalRepository, accountRepository, transactionRepository, clock);
    }

    @Bean
    public GetDashboardUseCase getDashboardUseCase(
            AccountRepository accountRepository,
            TransactionRepository transactionRepository,
            CategoryRepository categoryRepository,
            BudgetRepository budgetRepository,
            PulseScoreRepository pulseScoreRepository,
            IdGenerator idGenerator,
            Clock clock) {
        return new GetDashboardUseCase(
                accountRepository, transactionRepository, categoryRepository, budgetRepository, pulseScoreRepository, idGenerator, clock);
    }

    @Bean
    public GetPulseScoreHistoryUseCase getPulseScoreHistoryUseCase(PulseScoreRepository pulseScoreRepository, Clock clock) {
        return new GetPulseScoreHistoryUseCase(pulseScoreRepository, clock);
    }

    @Bean
    public GetSpendingByCategoryReportUseCase getSpendingByCategoryReportUseCase(
            TransactionRepository transactionRepository, CategoryRepository categoryRepository) {
        return new GetSpendingByCategoryReportUseCase(transactionRepository, categoryRepository);
    }

    @Bean
    public GetPeriodComparisonReportUseCase getPeriodComparisonReportUseCase(
            TransactionRepository transactionRepository, CategoryRepository categoryRepository) {
        return new GetPeriodComparisonReportUseCase(transactionRepository, categoryRepository);
    }

    @Bean
    public GetTransactionsForPeriodUseCase getTransactionsForPeriodUseCase(
            TransactionRepository transactionRepository, AccountRepository accountRepository, CategoryRepository categoryRepository) {
        return new GetTransactionsForPeriodUseCase(transactionRepository, accountRepository, categoryRepository);
    }

    @Bean
    public GetNotificationPreferencesUseCase getNotificationPreferencesUseCase(NotificationPreferenceRepository notificationPreferenceRepository) {
        return new GetNotificationPreferencesUseCase(notificationPreferenceRepository);
    }

    @Bean
    public UpdateNotificationPreferencesUseCase updateNotificationPreferencesUseCase(
            NotificationPreferenceRepository notificationPreferenceRepository, IdGenerator idGenerator) {
        return new UpdateNotificationPreferencesUseCase(notificationPreferenceRepository, idGenerator);
    }

    @Bean
    public CheckNotificationsUseCase checkNotificationsUseCase(
            ListBudgetsUseCase listBudgetsUseCase,
            ListGoalsUseCase listGoalsUseCase,
            TransactionRepository transactionRepository,
            CategoryRepository categoryRepository,
            UserRepository userRepository,
            NotificationRepository notificationRepository,
            NotificationPreferenceRepository notificationPreferenceRepository,
            AlertEmailNotifier alertEmailNotifier,
            IdGenerator idGenerator,
            Clock clock) {
        return new CheckNotificationsUseCase(
                listBudgetsUseCase, listGoalsUseCase, transactionRepository, categoryRepository, userRepository, notificationRepository,
                notificationPreferenceRepository, alertEmailNotifier, idGenerator, clock);
    }

    @Bean
    public ListNotificationsUseCase listNotificationsUseCase(NotificationRepository notificationRepository) {
        return new ListNotificationsUseCase(notificationRepository);
    }

    @Bean
    public MarkNotificationReadUseCase markNotificationReadUseCase(NotificationRepository notificationRepository) {
        return new MarkNotificationReadUseCase(notificationRepository);
    }

    @Bean
    public DeleteAccountUseCase deleteAccountUseCase(UserRepository userRepository, PasswordHasher passwordHasher, IdGenerator idGenerator) {
        return new DeleteAccountUseCase(userRepository, passwordHasher, idGenerator);
    }

    @Bean
    public RecordConsentUseCase recordConsentUseCase(ConsentRepository consentRepository, IdGenerator idGenerator) {
        return new RecordConsentUseCase(consentRepository, idGenerator);
    }

    @Bean
    public ListConsentHistoryUseCase listConsentHistoryUseCase(ConsentRepository consentRepository) {
        return new ListConsentHistoryUseCase(consentRepository);
    }

    @Bean
    public ExportUserDataUseCase exportUserDataUseCase(
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
        return new ExportUserDataUseCase(
                userRepository, accountRepository, transactionRepository, categoryRepository, budgetRepository, goalRepository,
                pulseScoreRepository, notificationRepository, notificationPreferenceRepository, consentRepository);
    }

    @Bean
    public GetUserForSupportUseCase getUserForSupportUseCase(
            UserRepository userRepository, ExportUserDataUseCase exportUserDataUseCase, AuditLogRepository auditLogRepository,
            IdGenerator idGenerator) {
        return new GetUserForSupportUseCase(userRepository, exportUserDataUseCase, auditLogRepository, idGenerator);
    }

    @Bean
    public SuspendAccountUseCase suspendAccountUseCase(
            UserRepository userRepository, AuditLogRepository auditLogRepository, IdGenerator idGenerator) {
        return new SuspendAccountUseCase(userRepository, auditLogRepository, idGenerator);
    }

    @Bean
    public ReactivateAccountUseCase reactivateAccountUseCase(
            UserRepository userRepository, AuditLogRepository auditLogRepository, IdGenerator idGenerator) {
        return new ReactivateAccountUseCase(userRepository, auditLogRepository, idGenerator);
    }

    @Bean
    public GetAuditLogUseCase getAuditLogUseCase(UserRepository userRepository, AuditLogRepository auditLogRepository) {
        return new GetAuditLogUseCase(userRepository, auditLogRepository);
    }
}
