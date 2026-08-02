package com.financepulse.engine.adapters.in.web.dto;

import com.financepulse.engine.application.usecases.notification.GetNotificationPreferencesUseCase.PreferenceView;
import com.financepulse.engine.domain.notification.AlertType;
import com.financepulse.engine.domain.notification.NotificationChannel;

public record NotificationPreferenceResponse(AlertType alertType, NotificationChannel channel, boolean enabled) {

    public static NotificationPreferenceResponse from(PreferenceView view) {
        return new NotificationPreferenceResponse(view.alertType(), view.channel(), view.enabled());
    }
}
