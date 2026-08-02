package com.financepulse.engine.application.usecases.backoffice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.financepulse.engine.domain.backoffice.errors.ForbiddenException;
import com.financepulse.engine.domain.user.Email;
import com.financepulse.engine.domain.user.Role;
import com.financepulse.engine.domain.user.User;
import com.financepulse.engine.testsupport.InMemoryAuditLogRepository;
import com.financepulse.engine.testsupport.InMemoryUserRepository;
import com.financepulse.engine.testsupport.SequentialIdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GetAuditLogUseCaseTest {

    private InMemoryUserRepository userRepository;
    private InMemoryAuditLogRepository auditLogRepository;
    private GetAuditLogUseCase useCase;
    private SuspendAccountUseCase suspendAccountUseCase;

    @BeforeEach
    void setUp() {
        userRepository = new InMemoryUserRepository();
        auditLogRepository = new InMemoryAuditLogRepository();
        useCase = new GetAuditLogUseCase(userRepository, auditLogRepository);
        suspendAccountUseCase = new SuspendAccountUseCase(userRepository, auditLogRepository, new SequentialIdGenerator("audit"));

        userRepository.save(User.register("operator-1", Email.create("operator@example.com"), "hash"));
        User operator = userRepository.findById("operator-1").orElseThrow();
        userRepository.update(User.reconstitute(
                operator.getId(), operator.getEmail(), operator.getPasswordHash(), operator.getName(), operator.getCreatedAt(), null,
                Role.SUPPORT_OPERATOR, null));

        userRepository.save(User.register("target-1", Email.create("target@example.com"), "hash"));
    }

    @Test
    void listsPreviouslyRecordedActionsForTheTargetUser() {
        suspendAccountUseCase.execute(new SuspendAccountUseCase.Input("operator-1", "target-1", "motivo"));

        GetAuditLogUseCase.Output output = useCase.execute(new GetAuditLogUseCase.Input("operator-1", "target-1"));

        assertThat(output.entries()).hasSize(1);
        assertThat(output.entries().get(0).operatorUserId()).isEqualTo("operator-1");
    }

    @Test
    void viewingTheAuditLogDoesNotCreateANewEntryInItself() {
        suspendAccountUseCase.execute(new SuspendAccountUseCase.Input("operator-1", "target-1", "motivo"));

        useCase.execute(new GetAuditLogUseCase.Input("operator-1", "target-1"));
        GetAuditLogUseCase.Output secondRead = useCase.execute(new GetAuditLogUseCase.Input("operator-1", "target-1"));

        assertThat(secondRead.entries()).hasSize(1);
    }

    @Test
    void rejectsAnOperatorWithoutTheSupportOperatorRole() {
        userRepository.save(User.register("customer-1", Email.create("customer@example.com"), "hash"));

        assertThatThrownBy(() -> useCase.execute(new GetAuditLogUseCase.Input("customer-1", "target-1"))).isInstanceOf(ForbiddenException.class);
    }
}
