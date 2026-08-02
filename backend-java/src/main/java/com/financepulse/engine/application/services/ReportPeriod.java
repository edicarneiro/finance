package com.financepulse.engine.application.services;

import com.financepulse.engine.domain.report.errors.InvalidReportPeriodException;
import java.time.LocalDate;

/**
 * RF-037/RF-038/RF-039: intervalo explícito fornecido pelo cliente para um
 * relatório — nunca um preset calculado pelo backend ("mês atual", "mês
 * anterior" são exemplos de uso, não uma prescrição de cálculo interno, ver
 * ADR-0021). Datas invertidas são rejeitadas explicitamente, não
 * reordenadas silenciosamente.
 */
public record ReportPeriod(LocalDate start, LocalDate end) {

    public ReportPeriod {
        if (start == null || end == null || start.isAfter(end)) {
            throw new InvalidReportPeriodException();
        }
    }

    public boolean contains(LocalDate date) {
        return !date.isBefore(start) && !date.isAfter(end);
    }
}
