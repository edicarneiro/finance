package com.financepulse.engine.adapters.in.web.dto;

import com.financepulse.engine.application.usecases.dashboard.GetDashboardUseCase.PulseScoreView;
import java.math.BigDecimal;
import java.util.List;

/** RF-036: {@code factors} entrega a explicabilidade — quais sinais compuseram o score e com qual peso (ver ADR-0020). */
public record PulseScoreResponse(BigDecimal overallScore, String formulaVersion, List<PulseScoreFactorResponse> factors) {

    public static PulseScoreResponse from(PulseScoreView view) {
        return new PulseScoreResponse(
                view.overallScore(), view.formulaVersion(), view.factors().stream().map(PulseScoreFactorResponse::from).toList());
    }
}
