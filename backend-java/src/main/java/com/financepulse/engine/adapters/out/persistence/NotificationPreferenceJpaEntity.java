package com.financepulse.engine.adapters.out.persistence;

import com.financepulse.engine.domain.notification.AlertType;
import com.financepulse.engine.domain.notification.NotificationChannel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "notification_preferences", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "alert_type", "channel"}))
public class NotificationPreferenceJpaEntity {

    @Id
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false)
    private AlertType alertType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationChannel channel;

    @Column(nullable = false)
    private boolean enabled;

    protected NotificationPreferenceJpaEntity() {
        // exigido pelo JPA/Hibernate
    }

    public NotificationPreferenceJpaEntity(String id, String userId, AlertType alertType, NotificationChannel channel, boolean enabled) {
        this.id = id;
        this.userId = userId;
        this.alertType = alertType;
        this.channel = channel;
        this.enabled = enabled;
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
