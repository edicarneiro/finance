package com.financepulse.engine.domain.notification;

/**
 * RF-040 a RF-042. RF-043 (lembrete de transação recorrente) não tem um
 * valor aqui — depende de transações recorrentes (RF-016, Fase 4.2), que
 * ainda não existem no domínio (ver ADR-0022).
 */
public enum AlertType {
    BUDGET_THRESHOLD,
    GOAL_THRESHOLD,
    ATYPICAL_SPENDING
}
