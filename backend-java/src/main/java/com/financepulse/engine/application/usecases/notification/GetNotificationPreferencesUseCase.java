package com.financepulse.engine.application.usecases.notification;

import com.financepulse.engine.application.ports.NotificationPreferenceRepository;
import com.financepulse.engine.application.services.NotificationPreferenceResolver;
import com.financepulse.engine.domain.notification.AlertType;
import com.financepulse.engine.domain.notification.NotificationChannel;
import com.financepulse.engine.domain.notification.NotificationPreference;
import java.util.List;

/**
 * RF-040: sempre retorna as 3 × 2 combinações completas (alertType ×
 * channel), mesclando linhas persistidas com o padrão {@code true} para as
 * ausentes — nenhuma escrita ocorre até o usuário de fato alterar uma
 * preferência (ver ADR-0022).
 */
public class GetNotificationPreferencesUseCase {

    private final NotificationPreferenceRepository notificationPreferenceRepository;

    public GetNotificationPreferencesUseCase(NotificationPreferenceRepository notificationPreferenceRepository) {
        this.notificationPreferenceRepository = notificationPreferenceRepository;
    }

    public Output execute(Input input) {
        List<NotificationPreference> preferences = notificationPreferenceRepository.findAllByUserId(input.userId());

        List<PreferenceView> views = List.of(AlertType.values()).stream()
                .flatMap(alertType -> List.of(NotificationChannel.values()).stream()
                        .map(channel -> new PreferenceView(alertType, channel, NotificationPreferenceResolver.isEnabled(preferences, alertType, channel))))
                .toList();

        return new Output(views);
    }

    public record Input(String userId) {
    }

    public record Output(List<PreferenceView> preferences) {
    }

    public record PreferenceView(AlertType alertType, NotificationChannel channel, boolean enabled) {
    }
}
