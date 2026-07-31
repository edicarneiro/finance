package com.financepulse.engine.application.usecases.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.financepulse.engine.domain.account.AccountType;
import com.financepulse.engine.domain.account.errors.AccountNotFoundException;
import com.financepulse.engine.testsupport.InMemoryAccountRepository;
import com.financepulse.engine.testsupport.SequentialIdGenerator;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ArchiveAccountUseCaseTest {

    private InMemoryAccountRepository accountRepository;
    private ArchiveAccountUseCase useCase;
    private String accountId;

    @BeforeEach
    void setUp() {
        accountRepository = new InMemoryAccountRepository();
        useCase = new ArchiveAccountUseCase(accountRepository);

        CreateAccountUseCase createAccount = new CreateAccountUseCase(accountRepository, new SequentialIdGenerator("account"));
        accountId = createAccount
                .execute(new CreateAccountUseCase.Input("user-1", AccountType.CASH, "Carteira", "BRL", BigDecimal.ZERO))
                .accountId();
    }

    @Test
    void archivesAnExistingAccountOwnedByTheUser() {
        useCase.execute(new ArchiveAccountUseCase.Input("user-1", accountId));

        assertThat(accountRepository.findByIdAndUserId(accountId, "user-1").get().isArchived()).isTrue();
    }

    @Test
    void archivingTwiceIsIdempotent() {
        useCase.execute(new ArchiveAccountUseCase.Input("user-1", accountId));

        useCase.execute(new ArchiveAccountUseCase.Input("user-1", accountId));

        assertThat(accountRepository.findByIdAndUserId(accountId, "user-1").get().isArchived()).isTrue();
    }

    @Test
    void rejectsArchivingANonExistentAccount() {
        assertThatThrownBy(() -> useCase.execute(new ArchiveAccountUseCase.Input("user-1", "ghost-account")))
                .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    void rejectsArchivingAnotherUsersAccountWithTheSameErrorAsNotFound() {
        assertThatThrownBy(() -> useCase.execute(new ArchiveAccountUseCase.Input("another-user", accountId)))
                .isInstanceOf(AccountNotFoundException.class);
    }
}
