package com.financepulse.engine.application.usecases.backoffice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.financepulse.engine.application.usecases.user.ExportUserDataUseCase;
import com.financepulse.engine.domain.account.Account;
import com.financepulse.engine.domain.account.AccountType;
import com.financepulse.engine.domain.account.Currency;
import com.financepulse.engine.domain.backoffice.AuditAction;
import com.financepulse.engine.domain.backoffice.errors.ForbiddenException;
import com.financepulse.engine.domain.user.Email;
import com.financepulse.engine.domain.user.Role;
import com.financepulse.engine.domain.user.User;
import com.financepulse.engine.testsupport.InMemoryAccountRepository;
import com.financepulse.engine.testsupport.InMemoryAuditLogRepository;
import com.financepulse.engine.testsupport.InMemoryBudgetRepository;
import com.financepulse.engine.testsupport.InMemoryCategoryRepository;
import com.financepulse.engine.testsupport.InMemoryConsentRepository;
import com.financepulse.engine.testsupport.InMemoryGoalRepository;
import com.financepulse.engine.testsupport.InMemoryNotificationPreferenceRepository;
import com.financepulse.engine.testsupport.InMemoryNotificationRepository;
import com.financepulse.engine.testsupport.InMemoryPulseScoreRepository;
import com.financepulse.engine.testsupport.InMemoryTransactionRepository;
import com.financepulse.engine.testsupport.InMemoryUserRepository;
import com.financepulse.engine.testsupport.SequentialIdGenerator;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GetUserForSupportUseCaseTest {

    private InMemoryUserRepository userRepository;
    private InMemoryAccountRepository accountRepository;
    private InMemoryAuditLogRepository auditLogRepository;
    private GetUserForSupportUseCase useCase;

    @BeforeEach
    void setUp() {
        userRepository = new InMemoryUserRepository();
        accountRepository = new InMemoryAccountRepository();
        auditLogRepository = new InMemoryAuditLogRepository();

        ExportUserDataUseCase exportUserDataUseCase = new ExportUserDataUseCase(
                userRepository, accountRepository, new InMemoryTransactionRepository(), new InMemoryCategoryRepository(),
                new InMemoryBudgetRepository(), new InMemoryGoalRepository(), new InMemoryPulseScoreRepository(),
                new InMemoryNotificationRepository(), new InMemoryNotificationPreferenceRepository(), new InMemoryConsentRepository());
        useCase = new GetUserForSupportUseCase(userRepository, exportUserDataUseCase, auditLogRepository, new SequentialIdGenerator("audit"));

        userRepository.save(User.register("operator-1", Email.create("operator@example.com"), "hash"));
        User operator = userRepository.findById("operator-1").orElseThrow();
        userRepository.update(User.reconstitute(
                operator.getId(), operator.getEmail(), operator.getPasswordHash(), operator.getName(), operator.getCreatedAt(), null,
                Role.SUPPORT_OPERATOR, null));

        userRepository.save(User.register("target-1", Email.create("target@example.com"), "hash"));
        accountRepository.save(Account.create("account-1", "target-1", AccountType.CHECKING, "Conta", Currency.create("BRL"), BigDecimal.ZERO));
    }

    @Test
    void returnsTheTargetUsersDataForAnAuthorizedOperator() {
        ExportUserDataUseCase.Output output = useCase.execute(new GetUserForSupportUseCase.Input("operator-1", "target-1"));

        assertThat(output.profile().email()).isEqualTo("target@example.com");
        assertThat(output.accounts()).hasSize(1);
    }

    @Test
    void recordsAnAuditLogEntryForTheAccess() {
        useCase.execute(new GetUserForSupportUseCase.Input("operator-1", "target-1"));

        var entries = auditLogRepository.findAllByTargetUserId("target-1");
        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).getOperatorUserId()).isEqualTo("operator-1");
        assertThat(entries.get(0).getAction()).isEqualTo(AuditAction.VIEWED_USER_DATA);
    }

    @Test
    void rejectsAnOperatorWithoutTheSupportOperatorRole() {
        userRepository.save(User.register("customer-1", Email.create("customer@example.com"), "hash"));

        assertThatThrownBy(() -> useCase.execute(new GetUserForSupportUseCase.Input("customer-1", "target-1")))
                .isInstanceOf(ForbiddenException.class);
        assertThat(auditLogRepository.findAllByTargetUserId("target-1")).isEmpty();
    }
}
