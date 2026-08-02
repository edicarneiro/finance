package com.financepulse.engine.adapters.in.web.dto;

import com.financepulse.engine.application.usecases.notification.CheckNotificationsUseCase.NotificationView;
import com.financepulse.engine.domain.notification.AlertType;

public record CheckedNotificationResponse(String id, AlertType alertType, String message) {

    public static CheckedNotificationResponse from(NotificationView view) {
        return new CheckedNotificationResponse(view.id(), view.alertType(), view.message());
    }
}
