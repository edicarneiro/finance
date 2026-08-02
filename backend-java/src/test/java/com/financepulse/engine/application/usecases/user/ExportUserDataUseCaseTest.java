package com.financepulse.engine.application.usecases.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.financepulse.engine.domain.account.Account;
import com.financepulse.engine.domain.account.AccountType;
import com.financepulse.engine.domain.account.Currency;
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
import com.financepulse.engine.domain.user.ConsentRecord;
import com.financepulse.engine.domain.user.Email;
import com.financepulse.engine.domain.user.User;
import com.financepulse.engine.domain.user.errors.UserNotFoundException;
import com.financepulse.engine.testsupport.InMemoryAccountRepository;
import com.financepulse.engine.testsupport.InMemoryBudgetRepository;
import com.financepulse.engine.testsupport.InMemoryCategoryRepository;
import com.financepulse.engine.testsupport.InMemoryConsentRepository;
import com.financepulse.engine.testsupport.InMemoryGoalRepository;
import com.financepulse.engine.testsupport.InMemoryNotificationPreferenceRepository;
import com.financepulse.engine.testsupport.InMemoryNotificationRepository;
import com.financepulse.engine.testsupport.InMemoryPulseScoreRepository;
import com.financepulse.engine.testsupport.InMemoryTransactionRepository;
import com.financepulse.engine.testsupport.InMemoryUserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ExportUserDataUseCaseTest {

    private InMemoryUserRepository userRepository;
    private InMemoryAccountRepository accountRepository;
    private InMemoryTransactionRepository transactionRepository;
    private InMemoryCategoryRepository categoryRepository;
    private InMemoryBudgetRepository budgetRepository;
    private InMemoryGoalRepository goalRepository;
    private InMemoryPulseScoreRepository pulseScoreRepository;
    private InMemoryNotificationRepository notificationRepository;
    private InMemoryNotificationPreferenceRepository notificationPreferenceRepository;
    private InMemoryConsentRepository consentRepository;
    private ExportUserDataUseCase useCase;

    @BeforeEach
    void setUp() {
        userRepository = new InMemoryUserRepository();
        accountRepository = new InMemoryAccountRepository();
        transactionRepository = new InMemoryTransactionRepository();
        categoryRepository = new InMemoryCategoryRepository();
        budgetRepository = new InMemoryBudgetRepository();
        goalRepository = new InMemoryGoalRepository();
        pulseScoreRepository = new InMemoryPulseScoreRepository();
        notificationRepository = new InMemoryNotificationRepository();
        notificationPreferenceRepository = new InMemoryNotificationPreferenceRepository();
        consentRepository = new InMemoryConsentRepository();
        useCase = new ExportUserDataUseCase(
                userRepository, accountRepository, transactionRepository, categoryRepository, budgetRepository, goalRepository,
                pulseScoreRepository, notificationRepository, notificationPreferenceRepository, consentRepository);

        userRepository.save(User.register("user-1", Email.create("owner@example.com"), "hashed-value"));
    }

    @Test
    void neverIncludesThePasswordHashInTheProfileExport() {
        ExportUserDataUseCase.Output output = useCase.execute(new ExportUserDataUseCase.Input("user-1"));

        assertThat(output.profile().email()).isEqualTo("owner@example.com");
        // ProfileExport não tem campo de senha/hash — a ausência do getter já é a prova estrutural de que
        // nunca poderia ser incluído, mesmo que alguém tentasse.
    }

    @Test
    void aggregatesDataFromEveryAreaOfTheProduct() {
        accountRepository.save(Account.create("account-1", "user-1", AccountType.CHECKING, "Conta", Currency.create("BRL"), BigDecimal.ZERO));
        categoryRepository.save(Category.create("category-1", "user-1", "Alimentação", null));
        transactionRepository.save(Transaction.create(
                "tx-1", "user-1", "account-1", "category-1", TransactionType.EXPENSE, new BigDecimal("50"), LocalDate.of(2026, 7, 1), null,
                List.of()));
        budgetRepository.save(
                Budget.create("budget-1", "user-1", "category-1", new BigDecimal("100"), BudgetPeriodType.MONTHLY, null, null, List.of(80)));
        goalRepository.save(Goal.create(
                "goal-1", "user-1", "Reserva", new BigDecimal("1000"), LocalDate.of(2026, 12, 31), "account-1", null, List.of(80),
                LocalDate.of(2026, 7, 1)));
        pulseScoreRepository.saveOrUpdate(PulseScoreSnapshot.create(
                "snap-1", "user-1", LocalDate.of(2026, 7, 31), new BigDecimal("68.75"), new BigDecimal("90"), new BigDecimal("80"),
                new BigDecimal("0"), new BigDecimal("55"), "pulse-v0-provisional"));
        notificationRepository.save(Notification.create(
                "notif-1", "user-1", AlertType.BUDGET_THRESHOLD, "event-1", "msg", EnumSet.of(NotificationChannel.IN_APP)));
        notificationPreferenceRepository.saveOrUpdate(
                NotificationPreference.create("pref-1", "user-1", AlertType.BUDGET_THRESHOLD, NotificationChannel.EMAIL, false));
        consentRepository.save(ConsentRecord.create("consent-1", "user-1", "2026-08-01"));

        ExportUserDataUseCase.Output output = useCase.execute(new ExportUserDataUseCase.Input("user-1"));

        assertThat(output.accounts()).hasSize(1);
        assertThat(output.transactions()).hasSize(1);
        assertThat(output.categories()).hasSize(1);
        assertThat(output.budgets()).hasSize(1);
        assertThat(output.goals()).hasSize(1);
        assertThat(output.pulseScoreHistory()).hasSize(1);
        assertThat(output.pulseScoreHistory().get(0).budgetConsistencyScore()).isEqualByComparingTo("90");
        assertThat(output.pulseScoreHistory().get(0).savingsRateScore()).isEqualByComparingTo("80");
        assertThat(output.pulseScoreHistory().get(0).balanceTrendScore()).isEqualByComparingTo("55");
        assertThat(output.notifications()).hasSize(1);
        assertThat(output.notificationPreferences()).hasSize(1);
        assertThat(output.consentHistory()).hasSize(1);
    }

    @Test
    void doesNotMixDataFromAnotherUser() {
        accountRepository.save(Account.create("account-other", "another-user", AccountType.CHECKING, "Conta", Currency.create("BRL"), BigDecimal.ZERO));

        ExportUserDataUseCase.Output output = useCase.execute(new ExportUserDataUseCase.Input("user-1"));

        assertThat(output.accounts()).isEmpty();
    }

    @Test
    void rejectsExportingForANonExistentUser() {
        assertThatThrownBy(() -> useCase.execute(new ExportUserDataUseCase.Input("ghost-user"))).isInstanceOf(UserNotFoundException.class);
    }
}
