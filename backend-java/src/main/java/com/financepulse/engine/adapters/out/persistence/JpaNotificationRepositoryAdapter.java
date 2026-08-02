package com.financepulse.engine.adapters.out.persistence;

import com.financepulse.engine.application.ports.NotificationRepository;
import com.financepulse.engine.domain.notification.Notification;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaNotificationRepositoryAdapter implements NotificationRepository {

    private final SpringDataNotificationJpaRepository jpaRepository;

    public JpaNotificationRepositoryAdapter(SpringDataNotificationJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public List<Notification> findAllByUserId(String userId) {
        return jpaRepository.findAllByUserId(userId).stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<Notification> findByIdAndUserId(String id, String userId) {
        return jpaRepository.findByIdAndUserId(id, userId).map(this::toDomain);
    }

    @Override
    public boolean existsByUserIdAndEventKey(String userId, String eventKey) {
        return jpaRepository.existsByUserIdAndEventKey(userId, eventKey);
    }

    @Override
    public void save(Notification notification) {
        jpaRepository.save(toEntity(notification));
    }

    @Override
    public void update(Notification notification) {
        jpaRepository.save(toEntity(notification));
    }

    private NotificationJpaEntity toEntity(Notification notification) {
        return new NotificationJpaEntity(
                notification.getId(),
                notification.getUserId(),
                notification.getAlertType(),
                notification.getEventKey(),
                notification.getMessage(),
                notification.getDeliveredChannels(),
                notification.isRead(),
                notification.getCreatedAt());
    }

    private Notification toDomain(NotificationJpaEntity entity) {
        return Notification.reconstitute(
                entity.getId(),
                entity.getUserId(),
                entity.getAlertType(),
                entity.getEventKey(),
                entity.getMessage(),
                entity.getDeliveredChannels(),
                entity.isRead(),
                entity.getCreatedAt());
    }
}
