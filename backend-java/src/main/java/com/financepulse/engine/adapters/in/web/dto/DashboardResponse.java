package com.financepulse.engine.adapters.in.web.dto;

import com.financepulse.engine.application.usecases.dashboard.GetDashboardUseCase.Output;
import java.math.BigDecimal;
import java.util.List;

public record DashboardResponse(
        BigDecimal consolidatedBalance, CashFlowResponse cashFlow, List<CategorySpendingResponse> spendingByCategory, PulseScoreResponse pulseScore) {

    public static DashboardResponse from(Output output) {
        return new DashboardResponse(
                output.consolidatedBalance(),
                CashFlowResponse.from(output.cashFlow()),
                output.spendingByCategory().stream().map(CategorySpendingResponse::from).toList(),
                PulseScoreResponse.from(output.pulseScore()));
    }
}
