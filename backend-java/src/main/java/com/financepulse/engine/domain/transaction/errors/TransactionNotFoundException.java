package com.financepulse.engine.domain.transaction.errors;

public class TransactionNotFoundException extends RuntimeException {

    public TransactionNotFoundException() {
        super("Transação não encontrada.");
    }
}
