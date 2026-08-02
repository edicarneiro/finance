package com.financepulse.engine.adapters.in.web;

import com.financepulse.engine.adapters.in.web.dto.ConsentRecordResponse;
import com.financepulse.engine.adapters.in.web.dto.RecordConsentRequest;
import com.financepulse.engine.adapters.in.web.dto.UserDataExportResponse;
import com.financepulse.engine.application.usecases.user.ExportUserDataUseCase;
import com.financepulse.engine.application.usecases.user.ListConsentHistoryUseCase;
import com.financepulse.engine.application.usecases.user.RecordConsentUseCase;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Rotas protegidas por {@link AuthenticationInterceptor}. RF-044/RF-046 (ver ADR-0023). */
@RestController
@RequestMapping("/privacy")
public class PrivacyController {

    private final ExportUserDataUseCase exportUserDataUseCase;
    private final RecordConsentUseCase recordConsentUseCase;
    private final ListConsentHistoryUseCase listConsentHistoryUseCase;

    public PrivacyController(
            ExportUserDataUseCase exportUserDataUseCase, RecordConsentUseCase recordConsentUseCase,
            ListConsentHistoryUseCase listConsentHistoryUseCase) {
        this.exportUserDataUseCase = exportUserDataUseCase;
        this.recordConsentUseCase = recordConsentUseCase;
        this.listConsentHistoryUseCase = listConsentHistoryUseCase;
    }

    @GetMapping("/export")
    public ResponseEntity<UserDataExportResponse> export(HttpServletRequest httpRequest) {
        String userId = AuthenticationInterceptor.getAuthenticatedUserId(httpRequest);

        ExportUserDataUseCase.Output output = exportUserDataUseCase.execute(new ExportUserDataUseCase.Input(userId));

        return ResponseEntity.ok(UserDataExportResponse.from(output));
    }

    @PostMapping("/consents")
    public ResponseEntity<ConsentRecordResponse> recordConsent(@Valid @RequestBody RecordConsentRequest request, HttpServletRequest httpRequest) {
        String userId = AuthenticationInterceptor.getAuthenticatedUserId(httpRequest);

        RecordConsentUseCase.Output output = recordConsentUseCase.execute(new RecordConsentUseCase.Input(userId, request.version()));

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ConsentRecordResponse(output.consentId(), request.version(), output.acceptedAt()));
    }

    @GetMapping("/consents")
    public ResponseEntity<List<ConsentRecordResponse>> listConsents(HttpServletRequest httpRequest) {
        String userId = AuthenticationInterceptor.getAuthenticatedUserId(httpRequest);

        ListConsentHistoryUseCase.Output output = listConsentHistoryUseCase.execute(new ListConsentHistoryUseCase.Input(userId));

        return ResponseEntity.ok(output.consents().stream().map(ConsentRecordResponse::from).toList());
    }
}
