package com.financepulse.engine.domain.backoffice;

/** RF-048/RF-049/RF-050 (ver ADR-0024): toda ação de backoffice sobre dados de um usuário. */
public enum AuditAction {
    VIEWED_USER_DATA,
    SUSPENDED_ACCOUNT,
    REACTIVATED_ACCOUNT
}
