package com.financepulse.engine.domain.account.errors;

public class InvalidCurrencyException extends RuntimeException {

    public InvalidCurrencyException(String rawValue) {
        super("\"" + rawValue + "\" não é um código de moeda ISO 4217 válido (3 letras, ex.: BRL).");
    }
}
