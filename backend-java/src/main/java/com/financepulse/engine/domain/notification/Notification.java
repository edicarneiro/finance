package com.financepulse.engine.domain.notification;

import java.time.Instant;
import java.util.Set;

/**
 * RF-041/RF-042 (e a entrega represada de RF-028/RF-032, ver ADR-0022).
 * {@code eventKey} é a identidade de deduplicação — determinística por
 * evento detectado (ex.: {@code "budget:{id}:period:{start}:threshold:80"}),
 * garantindo que {@code POST /notifications/check} seja idempotente.
 * {@code deliveredChannels} registra os canais habilitados no momento da
 * detecção; a notificação é sempre persistida (mesmo com o conjunto vazio),
 * pois a deduplicação depende dela existir independentemente de preferência.
 */
public final class Notification {

    private final String id;
    private final String userId;
    private final AlertType alertType;
    private final String eventKey;
    private final String message;
    private final Set<NotificationChannel> deliveredChannels;
    private final boolean read;
    private final Instant createdAt;

    private Notification(
            String id,
            String userId,
            AlertType alertType,
            String eventKey,
            String message,
            Set<NotificationChannel> deliveredChannels,
            boolean read,
            Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.alertType = alertType;
        this.eventKey = eventKey;
        this.message = message;
        this.deliveredChannels = Set.copyOf(deliveredChannels);
        this.read = read;
        this.createdAt = createdAt;
    }

    public static Notification create(
            String id, String userId, AlertType alertType, String eventKey, String message, Set<NotificationChannel> deliveredChannels) {
        return new Notification(id, userId, alertType, eventKey, message, deliveredChannels, false, Instant.now());
    }

    public static Notification reconstitute(
            String id,
            String userId,
            AlertType alertType,
            String eventKey,
            String message,
            Set<NotificationChannel> deliveredChannels,
            boolean read,
            Instant createdAt) {
        return new Notification(id, userId, alertType, eventKey, message, deliveredChannels, read, createdAt);
    }

    /** Idempotente — marcar uma notificação já lida como lida não é erro (mesmo padrão de {@code Account.archive()}). */
    public Notification markRead() {
        if (read) {
            return this;
        }
        return new Notification(id, userId, alertType, eventKey, message, deliveredChannels, true, createdAt);
    }

    public boolean isDeliveredVia(NotificationChannel channel) {
        return deliveredChannels.contains(channel);
    }

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public AlertType getAlertType() {
        return alertType;
    }

    public String getEventKey() {
        return eventKey;
    }

    public String getMessage() {
        return message;
    }

    public Set<NotificationChannel> getDeliveredChannels() {
        return deliveredChannels;
    }

    public boolean isRead() {
        return read;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
