package com.financepulse.engine.domain.notification;

/**
 * RF-040: uma linha por combinação (alertType, channel) explicitamente
 * configurada pelo usuário. Combinações ausentes usam o padrão
 * {@code enabled = true} (opt-out — ver ADR-0022), resolvido fora desta
 * classe pelos casos de uso que a consultam.
 */
public final class NotificationPreference {

    private final String id;
    private final String userId;
    private final AlertType alertType;
    private final NotificationChannel channel;
    private final boolean enabled;

    private NotificationPreference(String id, String userId, AlertType alertType, NotificationChannel channel, boolean enabled) {
        this.id = id;
        this.userId = userId;
        this.alertType = alertType;
        this.channel = channel;
        this.enabled = enabled;
    }

    public static NotificationPreference create(String id, String userId, AlertType alertType, NotificationChannel channel, boolean enabled) {
        return new NotificationPreference(id, userId, alertType, channel, enabled);
    }

    public static NotificationPreference reconstitute(String id, String userId, AlertType alertType, NotificationChannel channel, boolean enabled) {
        return new NotificationPreference(id, userId, alertType, channel, enabled);
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

    public NotificationChannel getChannel() {
        return channel;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
