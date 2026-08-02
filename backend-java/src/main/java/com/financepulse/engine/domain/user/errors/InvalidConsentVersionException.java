package com.financepulse.engine.domain.user.errors;

public class InvalidConsentVersionException extends RuntimeException {

    public InvalidConsentVersionException() {
        super("A versão do consentimento não pode ser vazia.");
    }
}
