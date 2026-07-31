package com.financepulse.engine.domain.account.errors;

public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException() {
        super("Conta não encontrada.");
    }
}
