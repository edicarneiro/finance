package com.financepulse.engine.domain.user.errors;

public class InvalidEmailException extends RuntimeException {

    public InvalidEmailException(String rawValue) {
        super("\"" + rawValue + "\" não é um endereço de e-mail válido.");
    }
}
