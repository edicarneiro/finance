package com.financepulse.engine.domain.account.errors;

public class InvalidAccountNameException extends RuntimeException {

    public InvalidAccountNameException() {
        super("O nome da conta deve ter entre 1 e 100 caracteres.");
    }
}
