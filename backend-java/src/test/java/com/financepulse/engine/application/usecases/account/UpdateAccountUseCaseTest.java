package com.financepulse.engine.application.usecases.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.financepulse.engine.domain.account.AccountType;
import com.financepulse.engine.domain.account.errors.AccountNotFoundException;
import com.financepulse.engine.domain.account.errors.InvalidAccountNameException;
import com.financepulse.engine.testsupport.InMemoryAccountRepository;
import com.financepulse.engine.testsupport.SequentialIdGenerator;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UpdateAccountUseCaseTest {

    private InMemoryAccountRepository accountRepository;
    private UpdateAccountUseCase useCase;
    private String accountId;

    @BeforeEach
    void setUp() {
        accountRepository = new InMemoryAccountRepository();
        useCase = new UpdateAccountUseCase(accountRepository);

        CreateAccountUseCase createAccount = new CreateAccountUseCase(accountRepository, new SequentialIdGenerator("account"));
        accountId = createAccount
                .execute(new CreateAccountUseCase.Input("user-1", AccountType.CHECKING, "Conta Corrente", "BRL", BigDecimal.ZERO))
                .accountId();
    }

    @Test
    void renamesAnExistingAccountOwnedByTheUser() {
        useCase.execute(new UpdateAccountUseCase.Input("user-1", accountId, "Conta Principal"));

        assertThat(accountRepository.findByIdAndUserId(accountId, "user-1").get().getName()).isEqualTo("Conta Principal");
    }

    @Test
    void rejectsAnInvalidName() {
        assertThatThrownBy(() -> useCase.execute(new UpdateAccountUseCase.Input("user-1", accountId, "   ")))
                .isInstanceOf(InvalidAccountNameException.class);
    }

    @Test
    void rejectsUpdatingANonExistentAccount() {
        assertThatThrownBy(() -> useCase.execute(new UpdateAccountUseCase.Input("user-1", "ghost-account", "Novo Nome")))
                .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    void rejectsUpdatingAnotherUsersAccountWithTheSameErrorAsNotFound() {
        assertThatThrownBy(() -> useCase.execute(new UpdateAccountUseCase.Input("another-user", accountId, "Novo Nome")))
                .isInstanceOf(AccountNotFoundException.class);
    }
}
