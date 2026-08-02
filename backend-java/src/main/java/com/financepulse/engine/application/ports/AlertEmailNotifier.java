package com.financepulse.engine.application.ports;

import com.financepulse.engine.domain.notification.AlertType;

/**
 * Porta nomeada pela intenção ("avisar sobre um alerta por e-mail"), não
 * "EmailSender" genérico — mesmo raciocínio de nomenclatura de
 * {@code PasswordResetNotifier} (ADR-0009). Nenhum provedor real de e-mail
 * está integrado a este projeto ainda; o adaptador desta fase apenas loga
 * (ver ADR-0022).
 */
public interface AlertEmailNotifier {

    void notify(String toEmail, AlertType alertType, String message);
}
