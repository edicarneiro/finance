package com.financepulse.engine.domain.user.errors;

public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException(String email) {
        super("Já existe uma conta cadastrada com o e-mail \"" + email + "\".");
    }
}
