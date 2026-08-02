package com.financepulse.engine.application.usecases.backoffice;

import com.financepulse.engine.application.ports.UserRepository;
import com.financepulse.engine.domain.backoffice.errors.ForbiddenException;
import com.financepulse.engine.domain.user.User;

/**
 * RF-049/RF-050 (ver ADR-0024): checagem de papel compartilhada pelos casos
 * de uso de backoffice deste pacote — não é uma porta pública, apenas evita
 * repetir a mesma verificação de segurança em três lugares.
 */
final class OperatorAuthorization {

    private OperatorAuthorization() {
    }

    static void requireSupportOperator(UserRepository userRepository, String operatorUserId) {
        User operator = userRepository.findById(operatorUserId).orElseThrow(ForbiddenException::new);

        // isSuspended()/isDeleted() são checados aqui, não só no login: um operador cujo próprio acesso foi
        // revogado não pode continuar agindo sobre outras contas com um JWT emitido antes da revogação
        // (dentro da janela de validade de 15 minutos, mesma limitação estrutural já aceita em todo o
        // projeto — ver ADR-0010/0023 — mas aqui o risco é maior por afetar contas de terceiros).
        if (!operator.isSupportOperator() || operator.isSuspended() || operator.isDeleted()) {
            throw new ForbiddenException();
        }
    }
}
