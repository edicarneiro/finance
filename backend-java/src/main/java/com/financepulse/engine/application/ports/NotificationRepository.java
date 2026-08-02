package com.financepulse.engine.application.ports;

import com.financepulse.engine.domain.notification.Notification;
import java.util.List;
import java.util.Optional;

/** Toda leitura/escrita é escopada por userId na própria assinatura (RF-047, rules.md § 4). */
public interface NotificationRepository {

    List<Notification> findAllByUserId(String userId);

    Optional<Notification> findByIdAndUserId(String id, String userId);

    /** Base da deduplicação de {@code POST /notifications/check} (ver ADR-0022) — um evento nunca gera duas notificações. */
    boolean existsByUserIdAndEventKey(String userId, String eventKey);

    void save(Notification notification);

    void update(Notification notification);
}
