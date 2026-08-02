package com.financepulse.engine.domain.backoffice.errors;

public class ForbiddenException extends RuntimeException {

    public ForbiddenException() {
        super("Acesso negado: esta ação exige permissão de operador de suporte.");
    }
}
