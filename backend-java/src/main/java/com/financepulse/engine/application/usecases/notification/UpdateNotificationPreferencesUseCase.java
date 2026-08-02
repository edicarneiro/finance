package com.financepulse.engine.application.usecases.notification;

import com.financepulse.engine.application.ports.IdGenerator;
import com.financepulse.engine.application.ports.NotificationPreferenceRepository;
import com.financepulse.engine.domain.notification.AlertType;
import com.financepulse.engine.domain.notification.NotificationChannel;
import com.financepulse.engine.domain.notification.NotificationPreference;
import java.util.List;

/** RF-040: atualização parcial — apenas as combinações informadas são alteradas. */
public class UpdateNotificationPreferencesUseCase {

    private final NotificationPreferenceRepository notificationPreferenceRepository;
    private final IdGenerator idGenerator;

    public UpdateNotificationPreferencesUseCase(NotificationPreferenceRepository notificationPreferenceRepository, IdGenerator idGenerator) {
        this.notificationPreferenceRepository = notificationPreferenceRepository;
        this.idGenerator = idGenerator;
    }

    public void execute(Input input) {
        for (PreferenceUpdate update : input.updates()) {
            NotificationPreference preference =
                    NotificationPreference.create(idGenerator.generate(), input.userId(), update.alertType(), update.channel(), update.enabled());
            notificationPreferenceRepository.saveOrUpdate(preference);
        }
    }

    public record Input(String userId, List<PreferenceUpdate> updates) {
    }

    public record PreferenceUpdate(AlertType alertType, NotificationChannel channel, boolean enabled) {
    }
}
