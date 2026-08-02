package com.financepulse.engine.application.usecases.backoffice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.financepulse.engine.domain.backoffice.AuditAction;
import com.financepulse.engine.domain.backoffice.errors.ForbiddenException;
import com.financepulse.engine.domain.user.Email;
import com.financepulse.engine.domain.user.Role;
import com.financepulse.engine.domain.user.User;
import com.financepulse.engine.domain.user.errors.UserNotFoundException;
import com.financepulse.engine.testsupport.InMemoryAuditLogRepository;
import com.financepulse.engine.testsupport.InMemoryUserRepository;
import com.financepulse.engine.testsupport.SequentialIdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SuspendAccountUseCaseTest {

    private InMemoryUserRepository userRepository;
    private InMemoryAuditLogRepository auditLogRepository;
    private SuspendAccountUseCase useCase;

    @BeforeEach
    void setUp() {
        userRepository = new InMemoryUserRepository();
        auditLogRepository = new InMemoryAuditLogRepository();
        useCase = new SuspendAccountUseCase(userRepository, auditLogRepository, new SequentialIdGenerator("audit"));

        userRepository.save(User.register("operator-1", Email.create("operator@example.com"), "hash"));
        userRepository.save(User.register("target-1", Email.create("target@example.com"), "hash"));
    }

    private void promoteOperatorToSupportOperator() {
        User operator = userRepository.findById("operator-1").orElseThrow();
        userRepository.update(User.reconstitute(
                operator.getId(), operator.getEmail(), operator.getPasswordHash(), operator.getName(), operator.getCreatedAt(), null,
                Role.SUPPORT_OPERATOR, null));
    }

    @Test
    void suspendsTheTargetAccountWhenTheOperatorHasTheSupportOperatorRole() {
        promoteOperatorToSupportOperator();

        useCase.execute(new SuspendAccountUseCase.Input("operator-1", "target-1", "Suspeita de fraude"));

        assertThat(userRepository.findById("target-1").orElseThrow().isSuspended()).isTrue();
    }

    @Test
    void recordsAnAuditLogEntryForTheSuspension() {
        promoteOperatorToSupportOperator();

        useCase.execute(new SuspendAccountUseCase.Input("operator-1", "target-1", "Suspeita de fraude"));

        var entries = auditLogRepository.findAllByTargetUserId("target-1");
        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).getOperatorUserId()).isEqualTo("operator-1");
        assertThat(entries.get(0).getAction()).isEqualTo(AuditAction.SUSPENDED_ACCOUNT);
        assertThat(entries.get(0).getDetails()).isEqualTo("Suspeita de fraude");
    }

    @Test
    void rejectsAnOperatorWithoutTheSupportOperatorRole() {
        assertThatThrownBy(() -> useCase.execute(new SuspendAccountUseCase.Input("operator-1", "target-1", "motivo")))
                .isInstanceOf(ForbiddenException.class);

        assertThat(userRepository.findById("target-1").orElseThrow().isSuspended()).isFalse();
    }

    @Test
    void rejectsANonExistentOperator() {
        assertThatThrownBy(() -> useCase.execute(new SuspendAccountUseCase.Input("ghost-operator", "target-1", "motivo")))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void rejectsAnOperatorWhoseOwnAccountHasBeenSuspended() {
        promoteOperatorToSupportOperator();
        User operator = userRepository.findById("operator-1").orElseThrow();
        userRepository.update(operator.suspend(java.time.Instant.now()));

        assertThatThrownBy(() -> useCase.execute(new SuspendAccountUseCase.Input("operator-1", "target-1", "motivo")))
                .isInstanceOf(ForbiddenException.class);
        assertThat(userRepository.findById("target-1").orElseThrow().isSuspended()).isFalse();
    }

    @Test
    void rejectsANonExistentTargetUser() {
        promoteOperatorToSupportOperator();

        assertThatThrownBy(() -> useCase.execute(new SuspendAccountUseCase.Input("operator-1", "ghost-target", "motivo")))
                .isInstanceOf(UserNotFoundException.class);
    }
}
