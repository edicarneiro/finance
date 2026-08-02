package com.financepulse.engine.adapters.out.persistence;

import com.financepulse.engine.domain.notification.AlertType;
import com.financepulse.engine.domain.notification.NotificationChannel;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;

@Entity
@Table(name = "notifications", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "event_key"}))
public class NotificationJpaEntity {

    @Id
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false)
    private AlertType alertType;

    @Column(name = "event_key", nullable = false)
    private String eventKey;

    @Column(nullable = false, length = 1000)
    private String message;

    // Eager: mapeado para o domínio fora do escopo de uma sessão Hibernate ativa (mesmo padrão de BudgetJpaEntity.alertThresholds).
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "notification_delivered_channels", joinColumns = @JoinColumn(name = "notification_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "channel")
    private Set<NotificationChannel> deliveredChannels = EnumSet.noneOf(NotificationChannel.class);

    @Column(nullable = false)
    private boolean read;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected NotificationJpaEntity() {
        // exigido pelo JPA/Hibernate
    }

    public NotificationJpaEntity(
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
        this.deliveredChannels = deliveredChannels.isEmpty() ? EnumSet.noneOf(NotificationChannel.class) : EnumSet.copyOf(deliveredChannels);
        this.read = read;
        this.createdAt = createdAt;
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
