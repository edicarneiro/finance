package com.financepulse.engine.adapters.out.notification;

import com.financepulse.engine.application.ports.AlertEmailNotifier;
import com.financepulse.engine.domain.notification.AlertType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Estande de desenvolvimento — apenas loga o alerta, nenhum provedor de
 * e-mail real integrado (mesmo padrão de {@code ConsolePasswordResetNotifier}
 * do backend TypeScript, ADR-0009). Deve ser substituído por um adaptador
 * real antes de qualquer implantação de produção (ver ADR-0022).
 */
@Component
public class ConsoleAlertEmailNotifier implements AlertEmailNotifier {

    private static final Logger logger = LoggerFactory.getLogger(ConsoleAlertEmailNotifier.class);

    @Override
    public void notify(String toEmail, AlertType alertType, String message) {
        logger.info("[ALERTA {}] Para: {} | Mensagem: {}", alertType, toEmail, message);
    }
}
