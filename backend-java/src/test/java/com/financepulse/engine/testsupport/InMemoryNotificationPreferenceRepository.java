package com.financepulse.engine.testsupport;

import com.financepulse.engine.application.ports.NotificationPreferenceRepository;
import com.financepulse.engine.domain.notification.NotificationPreference;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class InMemoryNotificationPreferenceRepository implements NotificationPreferenceRepository {

    private final Map<String, NotificationPreference> preferencesById = new LinkedHashMap<>();

    @Override
    public List<NotificationPreference> findAllByUserId(String userId) {
        return preferencesById.values().stream().filter(p -> p.getUserId().equals(userId)).toList();
    }

    @Override
    public void saveOrUpdate(NotificationPreference preference) {
        preferencesById.values().stream()
                .filter(existing -> existing.getUserId().equals(preference.getUserId()) && existing.getAlertType() == preference.getAlertType()
                        && existing.getChannel() == preference.getChannel())
                .map(NotificationPreference::getId)
                .findFirst()
                .ifPresent(preferencesById::remove);

        preferencesById.put(preference.getId(), preference);
    }
}
