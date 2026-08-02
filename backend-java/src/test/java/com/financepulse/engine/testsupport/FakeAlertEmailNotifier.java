package com.financepulse.engine.testsupport;

import com.financepulse.engine.application.ports.AlertEmailNotifier;
import com.financepulse.engine.domain.notification.AlertType;
import java.util.ArrayList;
import java.util.List;

public class FakeAlertEmailNotifier implements AlertEmailNotifier {

    private final List<SentEmail> sentEmails = new ArrayList<>();
    private boolean failing;

    @Override
    public void notify(String toEmail, AlertType alertType, String message) {
        if (failing) {
            throw new RuntimeException("Falha simulada de envio de e-mail");
        }
        sentEmails.add(new SentEmail(toEmail, alertType, message));
    }

    public void simulateFailure() {
        this.failing = true;
    }

    public List<SentEmail> sentEmails() {
        return sentEmails;
    }

    public record SentEmail(String toEmail, AlertType alertType, String message) {
    }
}
