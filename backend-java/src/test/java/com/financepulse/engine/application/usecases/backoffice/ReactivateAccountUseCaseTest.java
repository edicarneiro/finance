package com.financepulse.engine.application.usecases.backoffice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.financepulse.engine.domain.backoffice.AuditAction;
import com.financepulse.engine.domain.backoffice.errors.ForbiddenException;
import com.financepulse.engine.domain.user.Email;
import com.financepulse.engine.domain.user.Role;
import com.financepulse.engine.domain.user.User;
import com.financepulse.engine.testsupport.InMemoryAuditLogRepository;
import com.financepulse.engine.testsupport.InMemoryUserRepository;
import com.financepulse.engine.testsupport.SequentialIdGenerator;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReactivateAccountUseCaseTest {

    private InMemoryUserRepository userRepository;
    private InMemoryAuditLogRepository auditLogRepository;
    private ReactivateAccountUseCase useCase;

    @BeforeEach
    void setUp() {
        userRepository = new InMemoryUserRepository();
        auditLogRepository = new InMemoryAuditLogRepository();
        useCase = new ReactivateAccountUseCase(userRepository, auditLogRepository, new SequentialIdGenerator("audit"));

        userRepository.save(User.register("operator-1", Email.create("operator@example.com"), "hash"));
        User operator = userRepository.findById("operator-1").orElseThrow();
        userRepository.update(User.reconstitute(
                operator.getId(), operator.getEmail(), operator.getPasswordHash(), operator.getName(), operator.getCreatedAt(), null,
                Role.SUPPORT_OPERATOR, null));

        userRepository.save(User.register("target-1", Email.create("target@example.com"), "hash"));
        User target = userRepository.findById("target-1").orElseThrow();
        userRepository.update(target.suspend(Instant.now()));
    }

    @Test
    void reactivatesASuspendedAccount() {
        useCase.execute(new ReactivateAccountUseCase.Input("operator-1", "target-1", "Suspeita descartada"));

        assertThat(userRepository.findById("target-1").orElseThrow().isSuspended()).isFalse();
    }

    @Test
    void recordsAnAuditLogEntryForTheReactivation() {
        useCase.execute(new ReactivateAccountUseCase.Input("operator-1", "target-1", "Suspeita descartada"));

        var entries = auditLogRepository.findAllByTargetUserId("target-1");
        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).getAction()).isEqualTo(AuditAction.REACTIVATED_ACCOUNT);
    }

    @Test
    void rejectsAnOperatorWithoutTheSupportOperatorRole() {
        userRepository.save(User.register("customer-1", Email.create("customer@example.com"), "hash"));

        assertThatThrownBy(() -> useCase.execute(new ReactivateAccountUseCase.Input("customer-1", "target-1", "motivo")))
                .isInstanceOf(ForbiddenException.class);
        assertThat(userRepository.findById("target-1").orElseThrow().isSuspended()).isTrue();
    }
}
