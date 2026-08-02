package com.financepulse.engine.adapters.in.web.dto;

import com.financepulse.engine.application.usecases.notification.ListNotificationsUseCase.NotificationView;
import com.financepulse.engine.domain.notification.AlertType;
import java.time.Instant;

public record NotificationResponse(String id, AlertType alertType, String message, boolean read, Instant createdAt) {

    public static NotificationResponse from(NotificationView view) {
        return new NotificationResponse(view.id(), view.alertType(), view.message(), view.read(), view.createdAt());
    }
}
