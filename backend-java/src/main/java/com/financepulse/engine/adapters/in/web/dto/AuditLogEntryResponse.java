package com.financepulse.engine.adapters.in.web.dto;

import com.financepulse.engine.application.usecases.backoffice.GetAuditLogUseCase.EntryView;
import com.financepulse.engine.domain.backoffice.AuditAction;
import java.time.Instant;

public record AuditLogEntryResponse(String operatorUserId, AuditAction action, String details, Instant createdAt) {

    public static AuditLogEntryResponse from(EntryView view) {
        return new AuditLogEntryResponse(view.operatorUserId(), view.action(), view.details(), view.createdAt());
    }
}
