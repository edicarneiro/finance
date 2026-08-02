package com.financepulse.engine.domain.report.errors;

public class InvalidReportPeriodException extends RuntimeException {

    public InvalidReportPeriodException() {
        super("O período do relatório é inválido: a data inicial deve ser anterior ou igual à data final.");
    }
}
