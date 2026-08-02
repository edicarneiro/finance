package com.financepulse.engine.application.usecases.backoffice;

import com.financepulse.engine.application.ports.AuditLogRepository;
import com.financepulse.engine.application.ports.UserRepository;
import com.financepulse.engine.domain.backoffice.AuditAction;
import com.financepulse.engine.domain.backoffice.AuditLogEntry;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

/** RF-048/RF-049 (ver ADR-0024): consultar o log não gera uma nova entrada nele mesmo (evita ruído recursivo). */
public class GetAuditLogUseCase {

    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;

    public GetAuditLogUseCase(UserRepository userRepository, AuditLogRepository auditLogRepository) {
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
    }

    public Output execute(Input input) {
        OperatorAuthorization.requireSupportOperator(userRepository, input.operatorUserId());

        List<EntryView> entries = auditLogRepository.findAllByTargetUserId(input.targetUserId()).stream()
                .sorted(Comparator.comparing(AuditLogEntry::getCreatedAt).reversed())
                .map(entry -> new EntryView(entry.getOperatorUserId(), entry.getAction(), entry.getDetails(), entry.getCreatedAt()))
                .toList();

        return new Output(entries);
    }

    public record Input(String operatorUserId, String targetUserId) {
    }

    public record Output(List<EntryView> entries) {
    }

    public record EntryView(String operatorUserId, AuditAction action, String details, Instant createdAt) {
    }
}
