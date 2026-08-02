package com.financepulse.engine.application.usecases.notification;

import com.financepulse.engine.application.ports.NotificationRepository;
import com.financepulse.engine.domain.notification.Notification;
import com.financepulse.engine.domain.notification.errors.NotificationNotFoundException;

public class MarkNotificationReadUseCase {

    private final NotificationRepository notificationRepository;

    public MarkNotificationReadUseCase(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public void execute(Input input) {
        Notification notification =
                notificationRepository.findByIdAndUserId(input.notificationId(), input.userId()).orElseThrow(NotificationNotFoundException::new);

        notificationRepository.update(notification.markRead());
    }

    public record Input(String userId, String notificationId) {
    }
}
