package com.financepulse.engine.application.usecases.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.financepulse.engine.domain.account.Account;
import com.financepulse.engine.domain.account.AccountType;
import com.financepulse.engine.domain.account.errors.InvalidCurrencyException;
import com.financepulse.engine.testsupport.InMemoryAccountRepository;
import com.financepulse.engine.testsupport.SequentialIdGenerator;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CreateAccountUseCaseTest {

    private InMemoryAccountRepository accountRepository;
    private CreateAccountUseCase useCase;

    @BeforeEach
    void setUp() {
        accountRepository = new InMemoryAccountRepository();
        useCase = new CreateAccountUseCase(accountRepository, new SequentialIdGenerator("account"));
    }

    @Test
    void createsAnAccountWithTheInitialBalanceAsTheCurrentBalance() {
        CreateAccountUseCase.Output result = useCase.execute(
                new CreateAccountUseCase.Input("user-1", AccountType.CHECKING, "Conta Corrente", "BRL", new BigDecimal("100.00")));

        assertThat(result.accountId()).isEqualTo("account-1");

        Optional<Account> saved = accountRepository.findByIdAndUserId("account-1", "user-1");
        assertThat(saved).isPresent();
        assertThat(saved.get().getBalance()).isEqualByComparingTo("100.00");
        assertThat(saved.get().isArchived()).isFalse();
    }

    @Test
    void rejectsAnInvalidCurrencyCode() {
        assertThatThrownBy(() -> useCase.execute(
                        new CreateAccountUseCase.Input("user-1", AccountType.CASH, "Carteira", "REAIS", BigDecimal.ZERO)))
                .isInstanceOf(InvalidCurrencyException.class);
    }

    @Test
    void scopesTheCreatedAccountToTheProvidedUser() {
        useCase.execute(new CreateAccountUseCase.Input("user-1", AccountType.CASH, "Carteira", "BRL", BigDecimal.ZERO));

        assertThat(accountRepository.findByIdAndUserId("account-1", "another-user")).isEmpty();
    }
}
