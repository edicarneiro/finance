package com.financepulse.engine.application.usecases.backoffice;

import com.financepulse.engine.application.ports.AuditLogRepository;
import com.financepulse.engine.application.ports.IdGenerator;
import com.financepulse.engine.application.ports.UserRepository;
import com.financepulse.engine.domain.backoffice.AuditAction;
import com.financepulse.engine.domain.backoffice.AuditLogEntry;
import com.financepulse.engine.domain.user.User;
import com.financepulse.engine.domain.user.errors.UserNotFoundException;
import java.time.Instant;

/** RF-050 (ver ADR-0024): bloqueio de acesso reversível — não anonimiza dados (distinto de RF-045/ADR-0023). */
public class SuspendAccountUseCase {

    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final IdGenerator idGenerator;

    public SuspendAccountUseCase(UserRepository userRepository, AuditLogRepository auditLogRepository, IdGenerator idGenerator) {
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
        this.idGenerator = idGenerator;
    }

    public void execute(Input input) {
        OperatorAuthorization.requireSupportOperator(userRepository, input.operatorUserId());

        User target = userRepository.findById(input.targetUserId()).orElseThrow(UserNotFoundException::new);
        userRepository.update(target.suspend(Instant.now()));

        auditLogRepository.save(AuditLogEntry.create(
                idGenerator.generate(), input.operatorUserId(), input.targetUserId(), AuditAction.SUSPENDED_ACCOUNT, input.reason()));
    }

    public record Input(String operatorUserId, String targetUserId, String reason) {
    }
}
