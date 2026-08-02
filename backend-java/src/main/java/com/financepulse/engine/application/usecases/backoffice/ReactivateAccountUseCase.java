package com.financepulse.engine.application.usecases.backoffice;

import com.financepulse.engine.application.ports.AuditLogRepository;
import com.financepulse.engine.application.ports.IdGenerator;
import com.financepulse.engine.application.ports.UserRepository;
import com.financepulse.engine.domain.backoffice.AuditAction;
import com.financepulse.engine.domain.backoffice.AuditLogEntry;
import com.financepulse.engine.domain.user.User;
import com.financepulse.engine.domain.user.errors.UserNotFoundException;

/** RF-050 (ver ADR-0024): reverte uma suspensão — contraparte natural de {@link SuspendAccountUseCase}. */
public class ReactivateAccountUseCase {

    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final IdGenerator idGenerator;

    public ReactivateAccountUseCase(UserRepository userRepository, AuditLogRepository auditLogRepository, IdGenerator idGenerator) {
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
        this.idGenerator = idGenerator;
    }

    public void execute(Input input) {
        OperatorAuthorization.requireSupportOperator(userRepository, input.operatorUserId());

        User target = userRepository.findById(input.targetUserId()).orElseThrow(UserNotFoundException::new);
        userRepository.update(target.reactivate());

        auditLogRepository.save(AuditLogEntry.create(
                idGenerator.generate(), input.operatorUserId(), input.targetUserId(), AuditAction.REACTIVATED_ACCOUNT, input.reason()));
    }

    public record Input(String operatorUserId, String targetUserId, String reason) {
    }
}
