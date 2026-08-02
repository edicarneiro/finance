package com.financepulse.engine.application.usecases.backoffice;

import com.financepulse.engine.application.ports.AuditLogRepository;
import com.financepulse.engine.application.ports.IdGenerator;
import com.financepulse.engine.application.ports.UserRepository;
import com.financepulse.engine.application.usecases.user.ExportUserDataUseCase;
import com.financepulse.engine.domain.backoffice.AuditAction;
import com.financepulse.engine.domain.backoffice.AuditLogEntry;

/**
 * RF-049 (ver ADR-0024): investigação de suporte — reaproveita
 * {@link ExportUserDataUseCase} (Fase 11) em vez de duplicar a agregação de
 * dados pela segunda vez (mesmo padrão de composição de caso de uso já
 * validado em {@code CheckNotificationsUseCase}, ADR-0022). Todo acesso é
 * registrado em {@link AuditLogRepository} (RF-048).
 */
public class GetUserForSupportUseCase {

    private final UserRepository userRepository;
    private final ExportUserDataUseCase exportUserDataUseCase;
    private final AuditLogRepository auditLogRepository;
    private final IdGenerator idGenerator;

    public GetUserForSupportUseCase(
            UserRepository userRepository, ExportUserDataUseCase exportUserDataUseCase, AuditLogRepository auditLogRepository,
            IdGenerator idGenerator) {
        this.userRepository = userRepository;
        this.exportUserDataUseCase = exportUserDataUseCase;
        this.auditLogRepository = auditLogRepository;
        this.idGenerator = idGenerator;
    }

    public ExportUserDataUseCase.Output execute(Input input) {
        OperatorAuthorization.requireSupportOperator(userRepository, input.operatorUserId());

        ExportUserDataUseCase.Output output = exportUserDataUseCase.execute(new ExportUserDataUseCase.Input(input.targetUserId()));

        auditLogRepository.save(AuditLogEntry.create(
                idGenerator.generate(), input.operatorUserId(), input.targetUserId(), AuditAction.VIEWED_USER_DATA, null));

        return output;
    }

    public record Input(String operatorUserId, String targetUserId) {
    }
}
