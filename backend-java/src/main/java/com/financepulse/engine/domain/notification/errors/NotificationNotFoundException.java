package com.financepulse.engine.domain.notification.errors;

public class NotificationNotFoundException extends RuntimeException {

    public NotificationNotFoundException() {
        super("Notificação não encontrada.");
    }
}
