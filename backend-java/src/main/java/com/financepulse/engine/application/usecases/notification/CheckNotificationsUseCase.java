package com.financepulse.engine.application.usecases.notification;

import com.financepulse.engine.application.ports.AlertEmailNotifier;
import com.financepulse.engine.application.ports.CategoryRepository;
import com.financepulse.engine.application.ports.Clock;
import com.financepulse.engine.application.ports.IdGenerator;
import com.financepulse.engine.application.ports.NotificationPreferenceRepository;
import com.financepulse.engine.application.ports.NotificationRepository;
import com.financepulse.engine.application.ports.TransactionRepository;
import com.financepulse.engine.application.ports.UserRepository;
import com.financepulse.engine.application.services.AtypicalSpendingDetector;
import com.financepulse.engine.application.services.NotificationPreferenceResolver;
import com.financepulse.engine.application.usecases.budget.ListBudgetsUseCase;
import com.financepulse.engine.application.usecases.budget.ListBudgetsUseCase.BudgetView;
import com.financepulse.engine.application.usecases.goal.ListGoalsUseCase;
import com.financepulse.engine.application.usecases.goal.ListGoalsUseCase.GoalView;
import com.financepulse.engine.domain.category.Category;
import com.financepulse.engine.domain.notification.AlertType;
import com.financepulse.engine.domain.notification.Notification;
import com.financepulse.engine.domain.notification.NotificationChannel;
import com.financepulse.engine.domain.notification.NotificationPreference;
import com.financepulse.engine.domain.transaction.Transaction;
import com.financepulse.engine.domain.transaction.TransactionType;
import com.financepulse.engine.domain.user.User;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RF-041/RF-042 e a entrega represada de RF-028/RF-032 (ver ADR-0022).
 * Reaproveita {@link ListBudgetsUseCase}/{@link ListGoalsUseCase} (leitura,
 * sem efeito colateral) em vez de duplicar a orquestração de consumo de
 * orçamento/progresso de meta pela terceira vez. Sem scheduler dedicado —
 * só roda quando chamado (mesma limitação já aceita para o Pulse Score,
 * ADR-0020).
 */
public class CheckNotificationsUseCase {

    private static final Logger logger = LoggerFactory.getLogger(CheckNotificationsUseCase.class);
    private static final int ATYPICAL_SPENDING_WINDOW_DAYS = 30;

    private final ListBudgetsUseCase listBudgetsUseCase;
    private final ListGoalsUseCase listGoalsUseCase;
    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceRepository notificationPreferenceRepository;
    private final AlertEmailNotifier alertEmailNotifier;
    private final IdGenerator idGenerator;
    private final Clock clock;

    public CheckNotificationsUseCase(
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
        this.listBudgetsUseCase = listBudgetsUseCase;
        this.listGoalsUseCase = listGoalsUseCase;
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
        this.notificationPreferenceRepository = notificationPreferenceRepository;
        this.alertEmailNotifier = alertEmailNotifier;
        this.idGenerator = idGenerator;
        this.clock = clock;
    }

    public Output execute(Input input) {
        String userId = input.userId();
        List<NotificationPreference> preferences = notificationPreferenceRepository.findAllByUserId(userId);
        Map<String, String> categoryNames =
                categoryRepository.findAllByUserId(userId).stream().collect(Collectors.toMap(Category::getId, Category::getName));
        String userEmail = userRepository.findById(userId).map(User::getEmail).map(Object::toString).orElse(null);

        List<PendingEvent> events = new ArrayList<>();
        events.addAll(detectBudgetThresholdEvents(userId, categoryNames));
        events.addAll(detectGoalEvents(userId));
        events.addAll(detectAtypicalSpendingEvents(userId, categoryNames));

        List<Notification> created = new ArrayList<>();
        for (PendingEvent event : events) {
            if (notificationRepository.existsByUserIdAndEventKey(userId, event.eventKey())) {
                continue;
            }
            created.add(deliver(userId, userEmail, preferences, event));
        }

        return new Output(created.stream()
                .map(notification -> new NotificationView(notification.getId(), notification.getAlertType(), notification.getMessage()))
                .toList());
    }

    private Notification deliver(String userId, String userEmail, List<NotificationPreference> preferences, PendingEvent event) {
        Set<NotificationChannel> deliveredChannels = EnumSet.noneOf(NotificationChannel.class);

        if (NotificationPreferenceResolver.isEnabled(preferences, event.alertType(), NotificationChannel.IN_APP)) {
            deliveredChannels.add(NotificationChannel.IN_APP);
        }
        if (userEmail != null && NotificationPreferenceResolver.isEnabled(preferences, event.alertType(), NotificationChannel.EMAIL)) {
            try {
                alertEmailNotifier.notify(userEmail, event.alertType(), event.message());
                deliveredChannels.add(NotificationChannel.EMAIL);
            } catch (RuntimeException e) {
                logger.warn("Falha ao enviar notificação por e-mail para o evento {}", event.eventKey(), e);
            }
        }

        Notification notification = Notification.create(idGenerator.generate(), userId, event.alertType(), event.eventKey(), event.message(),
                deliveredChannels);
        notificationRepository.save(notification);

        return notification;
    }

    private List<PendingEvent> detectBudgetThresholdEvents(String userId, Map<String, String> categoryNames) {
        List<PendingEvent> events = new ArrayList<>();

        for (BudgetView budget : listBudgetsUseCase.execute(new ListBudgetsUseCase.Input(userId)).budgets()) {
            String categoryName = categoryNames.getOrDefault(budget.categoryId(), budget.categoryId());
            for (Integer threshold : budget.thresholdsCrossed()) {
                String eventKey = "budget:" + budget.id() + ":period:" + budget.periodStart() + ":threshold:" + threshold;
                String message = "Orçamento de \"" + categoryName + "\" atingiu " + threshold + "% do limite (R$ " + budget.consumedAmount()
                        + " de R$ " + budget.limitAmount() + ").";
                events.add(new PendingEvent(AlertType.BUDGET_THRESHOLD, eventKey, message));
            }
        }

        return events;
    }

    private List<PendingEvent> detectGoalEvents(String userId) {
        List<PendingEvent> events = new ArrayList<>();

        for (GoalView goal : listGoalsUseCase.execute(new ListGoalsUseCase.Input(userId)).goals()) {
            for (Integer threshold : goal.thresholdsCrossed()) {
                // O limiar 100 é redundante com o evento "achieved" abaixo quando a meta foi atingida — evita duas
                // notificações para o mesmo marco (ex.: thresholds=[80,100] e a meta é concluída).
                if (threshold == 100 && goal.achieved()) {
                    continue;
                }
                String eventKey = "goal:" + goal.id() + ":threshold:" + threshold;
                String message = "Meta \"" + goal.name() + "\" atingiu " + threshold + "% do valor-alvo.";
                events.add(new PendingEvent(AlertType.GOAL_THRESHOLD, eventKey, message));
            }
            if (goal.achieved()) {
                String eventKey = "goal:" + goal.id() + ":achieved";
                String message = "Meta \"" + goal.name() + "\" foi atingida!";
                events.add(new PendingEvent(AlertType.GOAL_THRESHOLD, eventKey, message));
            }
        }

        return events;
    }

    private List<PendingEvent> detectAtypicalSpendingEvents(String userId, Map<String, String> categoryNames) {
        LocalDate since = clock.today().minusDays(ATYPICAL_SPENDING_WINDOW_DAYS);
        List<Transaction> allExpenses = transactionRepository.findAllByUserId(userId).stream()
                .filter(transaction -> transaction.getType() == TransactionType.EXPENSE)
                .toList();

        List<PendingEvent> events = new ArrayList<>();
        for (Transaction candidate : allExpenses) {
            if (candidate.getDate().isBefore(since)) {
                continue;
            }
            List<BigDecimal> historicalAmounts = allExpenses.stream()
                    .filter(t -> t.getCategoryId().equals(candidate.getCategoryId()))
                    .filter(t -> t.getDate().isBefore(candidate.getDate()))
                    .map(Transaction::getAmount)
                    .toList();

            if (AtypicalSpendingDetector.isAtypical(historicalAmounts, candidate.getAmount())) {
                String categoryName = categoryNames.getOrDefault(candidate.getCategoryId(), candidate.getCategoryId());
                String eventKey = "transaction:" + candidate.getId() + ":atypical";
                String message = "Transação atípica detectada: R$ " + candidate.getAmount() + " em \"" + categoryName
                        + "\" — bem acima do seu padrão histórico.";
                events.add(new PendingEvent(AlertType.ATYPICAL_SPENDING, eventKey, message));
            }
        }

        return events;
    }

    public record Input(String userId) {
    }

    public record Output(List<NotificationView> newNotifications) {
    }

    public record NotificationView(String id, AlertType alertType, String message) {
    }

    private record PendingEvent(AlertType alertType, String eventKey, String message) {
    }
}
