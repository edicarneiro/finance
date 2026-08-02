package com.financepulse.engine.adapters.in.web.dto;

import com.financepulse.engine.domain.notification.AlertType;
import com.financepulse.engine.domain.notification.NotificationChannel;
import jakarta.validation.constraints.NotNull;

public record UpdateNotificationPreferenceRequest(@NotNull AlertType alertType, @NotNull NotificationChannel channel, boolean enabled) {
}
