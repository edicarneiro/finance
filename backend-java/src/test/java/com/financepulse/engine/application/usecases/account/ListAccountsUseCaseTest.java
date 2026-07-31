package com.financepulse.engine.application.usecases.account;

import static org.assertj.core.api.Assertions.assertThat;

import com.financepulse.engine.domain.account.AccountType;
import com.financepulse.engine.testsupport.InMemoryAccountRepository;
import com.financepulse.engine.testsupport.SequentialIdGenerator;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ListAccountsUseCaseTest {

    private InMemoryAccountRepository accountRepository;
    private CreateAccountUseCase createAccount;
    private ListAccountsUseCase useCase;

    @BeforeEach
    void setUp() {
        accountRepository = new InMemoryAccountRepository();
        createAccount = new CreateAccountUseCase(accountRepository, new SequentialIdGenerator("account"));
        useCase = new ListAccountsUseCase(accountRepository);
    }

    @Test
    void listsOnlyAccountsOwnedByTheUser() {
        createAccount.execute(new CreateAccountUseCase.Input("user-1", AccountType.CHECKING, "Conta A", "BRL", BigDecimal.TEN));
        createAccount.execute(new CreateAccountUseCase.Input("user-2", AccountType.CHECKING, "Conta B", "BRL", BigDecimal.ONE));

        ListAccountsUseCase.Output result = useCase.execute(new ListAccountsUseCase.Input("user-1"));

        assertThat(result.accounts()).hasSize(1);
        assertThat(result.accounts().get(0).name()).isEqualTo("Conta A");
    }

    @Test
    void includesArchivedAccountsInTheListing() {
        String accountId = createAccount
                .execute(new CreateAccountUseCase.Input("user-1", AccountType.CASH, "Carteira", "BRL", BigDecimal.ZERO))
                .accountId();
        new ArchiveAccountUseCase(accountRepository).execute(new ArchiveAccountUseCase.Input("user-1", accountId));

        ListAccountsUseCase.Output result = useCase.execute(new ListAccountsUseCase.Input("user-1"));

        assertThat(result.accounts()).hasSize(1);
        assertThat(result.accounts().get(0).archived()).isTrue();
    }

    @Test
    void returnsAnEmptyListWhenTheUserHasNoAccounts() {
        ListAccountsUseCase.Output result = useCase.execute(new ListAccountsUseCase.Input("user-without-accounts"));

        assertThat(result.accounts()).isEmpty();
    }
}
