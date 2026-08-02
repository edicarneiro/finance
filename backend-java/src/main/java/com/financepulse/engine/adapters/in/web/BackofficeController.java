package com.financepulse.engine.adapters.in.web;

import com.financepulse.engine.adapters.in.web.dto.AuditLogEntryResponse;
import com.financepulse.engine.adapters.in.web.dto.SuspendAccountRequest;
import com.financepulse.engine.adapters.in.web.dto.UserDataExportResponse;
import com.financepulse.engine.application.usecases.backoffice.GetAuditLogUseCase;
import com.financepulse.engine.application.usecases.backoffice.GetUserForSupportUseCase;
import com.financepulse.engine.application.usecases.backoffice.ReactivateAccountUseCase;
import com.financepulse.engine.application.usecases.backoffice.SuspendAccountUseCase;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;

/**
 * Rotas protegidas por {@link AuthenticationInterceptor} e, adicionalmente,
 * por checagem de papel (`SUPPORT_OPERATOR`) dentro de cada caso de uso.
 * RF-048/RF-049/RF-050 — versão mínima manual (ver ADR-0024).
 */
@RestController
@RequestMapping("/backoffice/users/{userId}")
public class BackofficeController {

    private final GetUserForSupportUseCase getUserForSupportUseCase;
    private final SuspendAccountUseCase suspendAccountUseCase;
    private final ReactivateAccountUseCase reactivateAccountUseCase;
    private final GetAuditLogUseCase getAuditLogUseCase;

    public BackofficeController(
            GetUserForSupportUseCase getUserForSupportUseCase,
            SuspendAccountUseCase suspendAccountUseCase,
            ReactivateAccountUseCase reactivateAccountUseCase,
            GetAuditLogUseCase getAuditLogUseCase) {
        this.getUserForSupportUseCase = getUserForSupportUseCase;
        this.suspendAccountUseCase = suspendAccountUseCase;
        this.reactivateAccountUseCase = reactivateAccountUseCase;
        this.getAuditLogUseCase = getAuditLogUseCase;
    }

    @GetMapping
    public ResponseEntity<UserDataExportResponse> getUserForSupport(@PathVariable String userId, HttpServletRequest httpRequest) {
        String operatorUserId = AuthenticationInterceptor.getAuthenticatedUserId(httpRequest);

        var output = getUserForSupportUseCase.execute(new GetUserForSupportUseCase.Input(operatorUserId, userId));

        return ResponseEntity.ok(UserDataExportResponse.from(output));
    }

    @PostMapping("/suspend")
    public ResponseEntity<Void> suspend(@PathVariable String userId, @RequestBody(required = false) SuspendAccountRequest request,
            HttpServletRequest httpRequest) {
        String operatorUserId = AuthenticationInterceptor.getAuthenticatedUserId(httpRequest);
        String reason = request == null ? null : request.reason();

        suspendAccountUseCase.execute(new SuspendAccountUseCase.Input(operatorUserId, userId, reason));

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reactivate")
    public ResponseEntity<Void> reactivate(@PathVariable String userId, @RequestBody(required = false) SuspendAccountRequest request,
            HttpServletRequest httpRequest) {
        String operatorUserId = AuthenticationInterceptor.getAuthenticatedUserId(httpRequest);
        String reason = request == null ? null : request.reason();

        reactivateAccountUseCase.execute(new ReactivateAccountUseCase.Input(operatorUserId, userId, reason));

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/audit-log")
    public ResponseEntity<List<AuditLogEntryResponse>> auditLog(@PathVariable String userId, HttpServletRequest httpRequest) {
        String operatorUserId = AuthenticationInterceptor.getAuthenticatedUserId(httpRequest);

        GetAuditLogUseCase.Output output = getAuditLogUseCase.execute(new GetAuditLogUseCase.Input(operatorUserId, userId));

        return ResponseEntity.ok(output.entries().stream().map(AuditLogEntryResponse::from).toList());
    }
}
