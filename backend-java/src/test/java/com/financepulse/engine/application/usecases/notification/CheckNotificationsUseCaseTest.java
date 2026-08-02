package com.financepulse.engine.application.usecases.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.financepulse.engine.application.usecases.budget.ListBudgetsUseCase;
import com.financepulse.engine.application.usecases.goal.ListGoalsUseCase;
import com.financepulse.engine.application.usecases.notification.CheckNotificationsUseCase.NotificationView;
import com.financepulse.engine.domain.account.Account;
import com.financepulse.engine.domain.account.AccountType;
import com.financepulse.engine.domain.account.Currency;
import com.financepulse.engine.domain.budget.Budget;
import com.financepulse.engine.domain.budget.BudgetPeriodType;
import com.financepulse.engine.domain.category.Category;
import com.financepulse.engine.domain.goal.Goal;
import com.financepulse.engine.domain.notification.AlertType;
import com.financepulse.engine.domain.notification.NotificationChannel;
import com.financepulse.engine.domain.notification.NotificationPreference;
import com.financepulse.engine.domain.transaction.Transaction;
import com.financepulse.engine.domain.transaction.TransactionType;
import com.financepulse.engine.domain.user.Email;
import com.financepulse.engine.domain.user.User;
import com.financepulse.engine.testsupport.FakeAlertEmailNotifier;
import com.financepulse.engine.testsupport.FixedClock;
import com.financepulse.engine.testsupport.InMemoryAccountRepository;
import com.financepulse.engine.testsupport.InMemoryBudgetRepository;
import com.financepulse.engine.testsupport.InMemoryCategoryRepository;
import com.financepulse.engine.testsupport.InMemoryGoalRepository;
import com.financepulse.engine.testsupport.InMemoryNotificationPreferenceRepository;
import com.financepulse.engine.testsupport.InMemoryNotificationRepository;
import com.financepulse.engine.testsupport.InMemoryTransactionRepository;
import com.financepulse.engine.testsupport.InMemoryUserRepository;
import com.financepulse.engine.testsupport.SequentialIdGenerator;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CheckNotificationsUseCaseTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 7, 31);

    private InMemoryAccountRepository accountRepository;
    private InMemoryTransactionRepository transactionRepository;
    private InMemoryCategoryRepository categoryRepository;
    private InMemoryBudgetRepository budgetRepository;
    private InMemoryGoalRepository goalRepository;
    private InMemoryUserRepository userRepository;
    private InMemoryNotificationRepository notificationRepository;
    private InMemoryNotificationPreferenceRepository notificationPreferenceRepository;
    private FakeAlertEmailNotifier alertEmailNotifier;
    private CheckNotificationsUseCase useCase;

    @BeforeEach
    void setUp() {
        accountRepository = new InMemoryAccountRepository();
        transactionRepository = new InMemoryTransactionRepository();
        categoryRepository = new InMemoryCategoryRepository();
        budgetRepository = new InMemoryBudgetRepository();
        goalRepository = new InMemoryGoalRepository();
        userRepository = new InMemoryUserRepository();
        notificationRepository = new InMemoryNotificationRepository();
        notificationPreferenceRepository = new InMemoryNotificationPreferenceRepository();
        alertEmailNotifier = new FakeAlertEmailNotifier();

        ListBudgetsUseCase listBudgetsUseCase = new ListBudgetsUseCase(budgetRepository, transactionRepository, new FixedClock(TODAY));
        ListGoalsUseCase listGoalsUseCase = new ListGoalsUseCase(goalRepository, accountRepository, transactionRepository, new FixedClock(TODAY));

        useCase = new CheckNotificationsUseCase(
                listBudgetsUseCase, listGoalsUseCase, transactionRepository, categoryRepository, userRepository, notificationRepository,
                notificationPreferenceRepository, alertEmailNotifier, new SequentialIdGenerator("notif"), new FixedClock(TODAY));

        userRepository.save(User.register("user-1", Email.create("owner@example.com"), "hash"));
    }

    @Test
    void notifiesWhenABudgetThresholdIsCrossed() {
        Category food = Category.create("category-food", "user-1", "Alimentação", null);
        categoryRepository.save(food);
        accountRepository.save(Account.create("account-1", "user-1", AccountType.CHECKING, "Conta", Currency.create("BRL"), BigDecimal.ZERO));
        budgetRepository.save(
                Budget.create("budget-1", "user-1", "category-food", new BigDecimal("100"), BudgetPeriodType.MONTHLY, null, null, List.of(80, 100)));
        transactionRepository.save(Transaction.create(
                "tx-1", "user-1", "account-1", "category-food", TransactionType.EXPENSE, new BigDecimal("90"), LocalDate.of(2026, 7, 15), null,
                List.of()));

        CheckNotificationsUseCase.Output output = useCase.execute(new CheckNotificationsUseCase.Input("user-1"));

        List<NotificationView> budgetNotifications =
                output.newNotifications().stream().filter(n -> n.alertType() == AlertType.BUDGET_THRESHOLD).toList();
        assertThat(budgetNotifications).hasSize(1);
        assertThat(budgetNotifications.get(0).message()).contains("Alimentação").contains("80%");
        assertThat(alertEmailNotifier.sentEmails()).hasSize(1);
        assertThat(alertEmailNotifier.sentEmails().get(0).toEmail()).isEqualTo("owner@example.com");
    }

    @Test
    void checkingTwiceDoesNotDuplicateTheSameBudgetThresholdNotification() {
        Category food = Category.create("category-food", "user-1", "Alimentação", null);
        categoryRepository.save(food);
        accountRepository.save(Account.create("account-1", "user-1", AccountType.CHECKING, "Conta", Currency.create("BRL"), BigDecimal.ZERO));
        budgetRepository.save(
                Budget.create("budget-1", "user-1", "category-food", new BigDecimal("100"), BudgetPeriodType.MONTHLY, null, null, List.of(80, 100)));
        transactionRepository.save(Transaction.create(
                "tx-1", "user-1", "account-1", "category-food", TransactionType.EXPENSE, new BigDecimal("90"), LocalDate.of(2026, 7, 15), null,
                List.of()));

        useCase.execute(new CheckNotificationsUseCase.Input("user-1"));
        CheckNotificationsUseCase.Output secondRun = useCase.execute(new CheckNotificationsUseCase.Input("user-1"));

        assertThat(secondRun.newNotifications()).isEmpty();
        assertThat(notificationRepository.findAllByUserId("user-1")).hasSize(1);
    }

    @Test
    void notifiesWhenAGoalIsAchieved() {
        accountRepository.save(Account.create("account-goal", "user-1", AccountType.SAVINGS, "Poupança", Currency.create("BRL"), new BigDecimal("1000")));
        goalRepository.save(Goal.create(
                "goal-1", "user-1", "Reserva", new BigDecimal("1000"), LocalDate.of(2026, 12, 31), "account-goal", null, List.of(80, 100), TODAY));

        CheckNotificationsUseCase.Output output = useCase.execute(new CheckNotificationsUseCase.Input("user-1"));

        List<NotificationView> achievedNotifications = output.newNotifications().stream()
                .filter(n -> n.alertType() == AlertType.GOAL_THRESHOLD && n.message().contains("atingida"))
                .toList();
        assertThat(achievedNotifications).hasSize(1);
        assertThat(achievedNotifications.get(0).message()).contains("Reserva");
    }

    @Test
    void doesNotDuplicateANotificationForTheHundredPercentThresholdWhenTheGoalIsAlreadyAchieved() {
        accountRepository.save(Account.create("account-goal", "user-1", AccountType.SAVINGS, "Poupança", Currency.create("BRL"), new BigDecimal("1000")));
        goalRepository.save(Goal.create(
                "goal-1", "user-1", "Reserva", new BigDecimal("1000"), LocalDate.of(2026, 12, 31), "account-goal", null, List.of(80, 100), TODAY));

        CheckNotificationsUseCase.Output output = useCase.execute(new CheckNotificationsUseCase.Input("user-1"));

        List<NotificationView> goalNotifications = output.newNotifications().stream().filter(n -> n.alertType() == AlertType.GOAL_THRESHOLD).toList();
        assertThat(goalNotifications).hasSize(2);
        assertThat(goalNotifications).extracting(NotificationView::message).noneMatch(m -> m.contains("100%"));
        assertThat(goalNotifications).extracting(NotificationView::message).anyMatch(m -> m.contains("80%"));
        assertThat(goalNotifications).extracting(NotificationView::message).anyMatch(m -> m.contains("atingida"));
    }

    @Test
    void notifiesAnAtypicalExpenseFarAboveTheHistoricalAverageInTheSameCategory() {
        Category shopping = Category.create("category-shopping", "user-1", "Compras", null);
        categoryRepository.save(shopping);
        accountRepository.save(Account.create("account-1", "user-1", AccountType.CHECKING, "Conta", Currency.create("BRL"), BigDecimal.ZERO));
        for (int day = 1; day <= 5; day++) {
            transactionRepository.save(Transaction.create(
                    "tx-history-" + day, "user-1", "account-1", "category-shopping", TransactionType.EXPENSE, new BigDecimal("10"),
                    LocalDate.of(2026, 1, day), null, List.of()));
        }
        transactionRepository.save(Transaction.create(
                "tx-outlier", "user-1", "account-1", "category-shopping", TransactionType.EXPENSE, new BigDecimal("1000"), LocalDate.of(2026, 7, 20),
                null, List.of()));

        CheckNotificationsUseCase.Output output = useCase.execute(new CheckNotificationsUseCase.Input("user-1"));

        List<NotificationView> atypicalNotifications =
                output.newNotifications().stream().filter(n -> n.alertType() == AlertType.ATYPICAL_SPENDING).toList();
        assertThat(atypicalNotifications).hasSize(1);
        assertThat(atypicalNotifications.get(0).message()).contains("Compras").contains("1000");
    }

    @Test
    void doesNotSendEmailWhenTheEmailChannelIsDisabledForTheAlertTypeButStillDeliversInApp() {
        notificationPreferenceRepository.saveOrUpdate(
                NotificationPreference.create("pref-1", "user-1", AlertType.BUDGET_THRESHOLD, NotificationChannel.EMAIL, false));
        Category food = Category.create("category-food", "user-1", "Alimentação", null);
        categoryRepository.save(food);
        accountRepository.save(Account.create("account-1", "user-1", AccountType.CHECKING, "Conta", Currency.create("BRL"), BigDecimal.ZERO));
        budgetRepository.save(
                Budget.create("budget-1", "user-1", "category-food", new BigDecimal("100"), BudgetPeriodType.MONTHLY, null, null, List.of(80, 100)));
        transactionRepository.save(Transaction.create(
                "tx-1", "user-1", "account-1", "category-food", TransactionType.EXPENSE, new BigDecimal("90"), LocalDate.of(2026, 7, 15), null,
                List.of()));

        useCase.execute(new CheckNotificationsUseCase.Input("user-1"));

        assertThat(alertEmailNotifier.sentEmails()).isEmpty();
        assertThat(notificationRepository.findAllByUserId("user-1").get(0).isDeliveredVia(NotificationChannel.IN_APP)).isTrue();
    }

    @Test
    void stillPersistsAndDedupesAnEventEvenWhenBothChannelsAreDisabled() {
        notificationPreferenceRepository.saveOrUpdate(
                NotificationPreference.create("pref-1", "user-1", AlertType.BUDGET_THRESHOLD, NotificationChannel.EMAIL, false));
        notificationPreferenceRepository.saveOrUpdate(
                NotificationPreference.create("pref-2", "user-1", AlertType.BUDGET_THRESHOLD, NotificationChannel.IN_APP, false));
        Category food = Category.create("category-food", "user-1", "Alimentação", null);
        categoryRepository.save(food);
        accountRepository.save(Account.create("account-1", "user-1", AccountType.CHECKING, "Conta", Currency.create("BRL"), BigDecimal.ZERO));
        budgetRepository.save(
                Budget.create("budget-1", "user-1", "category-food", new BigDecimal("100"), BudgetPeriodType.MONTHLY, null, null, List.of(80, 100)));
        transactionRepository.save(Transaction.create(
                "tx-1", "user-1", "account-1", "category-food", TransactionType.EXPENSE, new BigDecimal("90"), LocalDate.of(2026, 7, 15), null,
                List.of()));

        useCase.execute(new CheckNotificationsUseCase.Input("user-1"));
        CheckNotificationsUseCase.Output secondRun = useCase.execute(new CheckNotificationsUseCase.Input("user-1"));

        assertThat(notificationRepository.findAllByUserId("user-1")).hasSize(1);
        assertThat(notificationRepository.findAllByUserId("user-1").get(0).getDeliveredChannels()).isEmpty();
        assertThat(secondRun.newNotifications()).isEmpty();
    }

    @Test
    void aFailingEmailSendDoesNotPreventTheNotificationFromBeingCreatedOrOtherEventsFromBeingProcessed() {
        alertEmailNotifier.simulateFailure();
        Category food = Category.create("category-food", "user-1", "Alimentação", null);
        categoryRepository.save(food);
        accountRepository.save(Account.create("account-1", "user-1", AccountType.CHECKING, "Conta", Currency.create("BRL"), BigDecimal.ZERO));
        budgetRepository.save(
                Budget.create("budget-1", "user-1", "category-food", new BigDecimal("100"), BudgetPeriodType.MONTHLY, null, null, List.of(80, 100)));
        transactionRepository.save(Transaction.create(
                "tx-1", "user-1", "account-1", "category-food", TransactionType.EXPENSE, new BigDecimal("90"), LocalDate.of(2026, 7, 15), null,
                List.of()));

        CheckNotificationsUseCase.Output output = useCase.execute(new CheckNotificationsUseCase.Input("user-1"));

        assertThat(output.newNotifications()).hasSize(1);
        assertThat(notificationRepository.findAllByUserId("user-1").get(0).isDeliveredVia(NotificationChannel.IN_APP)).isTrue();
        assertThat(notificationRepository.findAllByUserId("user-1").get(0).isDeliveredVia(NotificationChannel.EMAIL)).isFalse();
    }

    @Test
    void returnsNoNotificationsForAUserWithNoBudgetsGoalsOrTransactions() {
        CheckNotificationsUseCase.Output output = useCase.execute(new CheckNotificationsUseCase.Input("user-1"));

        assertThat(output.newNotifications()).isEmpty();
    }
}
