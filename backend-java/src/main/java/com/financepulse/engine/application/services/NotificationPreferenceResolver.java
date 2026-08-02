package com.financepulse.engine.application.services;

import com.financepulse.engine.domain.notification.AlertType;
import com.financepulse.engine.domain.notification.NotificationChannel;
import com.financepulse.engine.domain.notification.NotificationPreference;
import java.util.List;

/**
 * RF-040: resolve se uma combinação (alertType, channel) está habilitada,
 * usando o padrão opt-out (`true`) quando o usuário não configurou nada
 * explicitamente (ver ADR-0022). Única fonte da verdade sobre o padrão,
 * reaproveitada tanto na leitura de preferências quanto na detecção de
 * eventos.
 */
public final class NotificationPreferenceResolver {

    private NotificationPreferenceResolver() {
    }

    public static boolean isEnabled(List<NotificationPreference> preferences, AlertType alertType, NotificationChannel channel) {
        return preferences.stream()
                .filter(preference -> preference.getAlertType() == alertType && preference.getChannel() == channel)
                .findFirst()
                .map(NotificationPreference::isEnabled)
                .orElse(true);
    }
}
