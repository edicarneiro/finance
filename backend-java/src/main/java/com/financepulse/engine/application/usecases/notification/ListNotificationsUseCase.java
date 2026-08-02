package com.financepulse.engine.application.usecases.notification;

import com.financepulse.engine.application.ports.NotificationRepository;
import com.financepulse.engine.domain.notification.AlertType;
import com.financepulse.engine.domain.notification.Notification;
import com.financepulse.engine.domain.notification.NotificationChannel;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

/** RF-040 (caixa de entrada in-app): só retorna notificações entregues via IN_APP, mais recentes primeiro. */
public class ListNotificationsUseCase {

    private final NotificationRepository notificationRepository;

    public ListNotificationsUseCase(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public Output execute(Input input) {
        List<NotificationView> views = notificationRepository.findAllByUserId(input.userId()).stream()
                .filter(notification -> notification.isDeliveredVia(NotificationChannel.IN_APP))
                .filter(notification -> !input.unreadOnly() || !notification.isRead())
                .sorted(Comparator.comparing(Notification::getCreatedAt).reversed())
                .map(notification -> new NotificationView(
                        notification.getId(), notification.getAlertType(), notification.getMessage(), notification.isRead(),
                        notification.getCreatedAt()))
                .toList();

        return new Output(views);
    }

    public record Input(String userId, boolean unreadOnly) {

        public Input(String userId) {
            this(userId, false);
        }
    }

    public record Output(List<NotificationView> notifications) {
    }

    public record NotificationView(String id, AlertType alertType, String message, boolean read, Instant createdAt) {
    }
}
