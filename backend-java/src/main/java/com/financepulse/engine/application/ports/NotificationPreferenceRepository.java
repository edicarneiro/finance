package com.financepulse.engine.application.ports;

import com.financepulse.engine.domain.notification.NotificationPreference;
import java.util.List;

/** Toda leitura/escrita é escopada por userId na própria assinatura (RF-047, rules.md § 4). */
public interface NotificationPreferenceRepository {

    List<NotificationPreference> findAllByUserId(String userId);

    /** Upsert por (userId, alertType, channel) — no máximo uma linha por combinação (ver ADR-0022). */
    void saveOrUpdate(NotificationPreference preference);
}
