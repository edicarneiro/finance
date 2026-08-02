package com.financepulse.engine.adapters.in.web.dto;

import com.financepulse.engine.application.usecases.user.ListConsentHistoryUseCase.ConsentView;
import java.time.Instant;

public record ConsentRecordResponse(String id, String version, Instant acceptedAt) {

    public static ConsentRecordResponse from(ConsentView view) {
        return new ConsentRecordResponse(view.id(), view.version(), view.acceptedAt());
    }
}
