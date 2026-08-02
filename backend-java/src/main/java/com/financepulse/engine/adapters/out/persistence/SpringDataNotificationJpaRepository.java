package com.financepulse.engine.adapters.out.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataNotificationJpaRepository extends JpaRepository<NotificationJpaEntity, String> {

    List<NotificationJpaEntity> findAllByUserId(String userId);

    Optional<NotificationJpaEntity> findByIdAndUserId(String id, String userId);

    boolean existsByUserIdAndEventKey(String userId, String eventKey);
}
