package com.financepulse.engine.adapters.in.web.dto;

import com.financepulse.engine.application.services.PulseScoreCalculator.Factor;
import java.math.BigDecimal;

public record PulseScoreFactorResponse(String name, BigDecimal score, BigDecimal weight) {

    public static PulseScoreFactorResponse from(Factor factor) {
        return new PulseScoreFactorResponse(factor.name(), factor.score(), factor.weight());
    }
}
