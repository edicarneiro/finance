package com.financepulse.engine.domain.budget;

/** RF-026. MONTHLY/WEEKLY são recorrentes (período vigente calculado a partir da data atual); CUSTOM é um intervalo fixo (ver ADR-0018). */
public enum BudgetPeriodType {
    MONTHLY,
    WEEKLY,
    CUSTOM
}
