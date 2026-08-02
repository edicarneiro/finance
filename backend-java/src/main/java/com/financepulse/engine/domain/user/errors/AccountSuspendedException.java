package com.financepulse.engine.domain.user.errors;

public class AccountSuspendedException extends RuntimeException {

    public AccountSuspendedException() {
        super("Esta conta foi suspensa. Entre em contato com o suporte.");
    }
}
