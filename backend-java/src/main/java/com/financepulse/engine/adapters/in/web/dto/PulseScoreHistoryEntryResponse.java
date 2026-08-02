package com.financepulse.engine.adapters.in.web.dto;

import com.financepulse.engine.application.usecases.dashboard.GetPulseScoreHistoryUseCase.HistoryEntry;
import java.math.BigDecimal;
import java.time.LocalDate;

public record PulseScoreHistoryEntryResponse(LocalDate date, BigDecimal score, String formulaVersion) {

    public static PulseScoreHistoryEntryResponse from(HistoryEntry entry) {
        return new PulseScoreHistoryEntryResponse(entry.date(), entry.score(), entry.formulaVersion());
    }
}
