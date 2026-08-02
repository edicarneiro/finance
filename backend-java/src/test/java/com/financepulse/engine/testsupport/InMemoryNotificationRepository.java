package com.financepulse.engine.testsupport;

import com.financepulse.engine.application.ports.NotificationRepository;
import com.financepulse.engine.domain.notification.Notification;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InMemoryNotificationRepository implements NotificationRepository {

    private final Map<String, Notification> notificationsById = new LinkedHashMap<>();

    @Override
    public List<Notification> findAllByUserId(String userId) {
        return notificationsById.values().stream().filter(n -> n.getUserId().equals(userId)).toList();
    }

    @Override
    public Optional<Notification> findByIdAndUserId(String id, String userId) {
        return Optional.ofNullable(notificationsById.get(id)).filter(n -> n.getUserId().equals(userId));
    }

    @Override
    public boolean existsByUserIdAndEventKey(String userId, String eventKey) {
        return notificationsById.values().stream().anyMatch(n -> n.getUserId().equals(userId) && n.getEventKey().equals(eventKey));
    }

    @Override
    public void save(Notification notification) {
        notificationsById.put(notification.getId(), notification);
    }

    @Override
    public void update(Notification notification) {
        notificationsById.put(notification.getId(), notification);
    }
}
