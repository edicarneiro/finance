package com.financepulse.engine.application.ports;

import java.time.LocalDate;

/** Torna "data atual" testável — primeira necessidade real no backend Java, para cálculo de período de orçamento (ADR-0018). */
public interface Clock {

    LocalDate today();
}
