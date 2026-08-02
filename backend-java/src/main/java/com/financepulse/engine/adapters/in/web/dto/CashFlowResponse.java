package com.financepulse.engine.adapters.in.web.dto;

import com.financepulse.engine.application.usecases.dashboard.GetDashboardUseCase.CashFlow;
import java.math.BigDecimal;

public record CashFlowResponse(int windowDays, BigDecimal totalIncome, BigDecimal totalExpense, BigDecimal net) {

    public static CashFlowResponse from(CashFlow cashFlow) {
        return new CashFlowResponse(cashFlow.windowDays(), cashFlow.totalIncome(), cashFlow.totalExpense(), cashFlow.net());
    }
}
