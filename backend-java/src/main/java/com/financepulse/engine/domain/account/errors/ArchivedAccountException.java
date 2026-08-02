package com.financepulse.engine.domain.account.errors;

/** ADR-0016: uma conta arquivada não aceita novos lançamentos. */
public class ArchivedAccountException extends RuntimeException {

    public ArchivedAccountException() {
        super("Não é possível lançar uma transação em uma conta arquivada.");
    }
}
