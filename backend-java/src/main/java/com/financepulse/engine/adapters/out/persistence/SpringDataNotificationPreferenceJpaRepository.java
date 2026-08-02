package com.financepulse.engine.adapters.out.persistence;

import com.financepulse.engine.domain.notification.AlertType;
import com.financepulse.engine.domain.notification.NotificationChannel;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataNotificationPreferenceJpaRepository extends JpaRepository<NotificationPreferenceJpaEntity, String> {

    List<NotificationPreferenceJpaEntity> findAllByUserId(String userId);

    Optional<NotificationPreferenceJpaEntity> findByUserIdAndAlertTypeAndChannel(String userId, AlertType alertType, NotificationChannel channel);
}
