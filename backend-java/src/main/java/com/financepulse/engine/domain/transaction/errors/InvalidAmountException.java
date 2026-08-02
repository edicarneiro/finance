package com.financepulse.engine.domain.transaction.errors;

public class InvalidAmountException extends RuntimeException {

    public InvalidAmountException() {
        super("O valor da transação deve ser maior que zero.");
    }
}
