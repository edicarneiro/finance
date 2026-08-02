package com.financepulse.engine.application.usecases.account;

import static org.assertj.core.api.Assertions.assertThat;

import com.financepulse.engine.domain.account.AccountType;
import com.financepulse.engine.domain.transaction.Transaction;
import com.financepulse.engine.domain.transaction.TransactionType;
import com.financepulse.engine.testsupport.InMemoryAccountRepository;
import com.financepulse.engine.testsupport.InMemoryTransactionRepository;
import com.financepulse.engine.testsupport.SequentialIdGenerator;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GetConsolidatedBalanceUseCaseTest {

    private InMemoryAccountRepository accountRepository;
    private InMemoryTransactionRepository transactionRepository;
    private CreateAccountUseCase createAccount;
    private GetConsolidatedBalanceUseCase useCase;

    @BeforeEach
    void setUp() {
        accountRepository = new InMemoryAccountRepository();
        transactionRepository = new InMemoryTransactionRepository();
        createAccount = new CreateAccountUseCase(accountRepository, new SequentialIdGenerator("account"));
        useCase = new GetConsolidatedBalanceUseCase(accountRepository, transactionRepository);
    }

    @Test
    void sumsTheBalanceOfAllActiveAccounts() {
        createAccount.execute(new CreateAccountUseCase.Input("user-1", AccountType.CHECKING, "Conta A", "BRL", new BigDecimal("100.00")));
        createAccount.execute(new CreateAccountUseCase.Input("user-1", AccountType.SAVINGS, "Conta B", "BRL", new BigDecimal("250.50")));

        GetConsolidatedBalanceUseCase.Output result = useCase.execute(new GetConsolidatedBalanceUseCase.Input("user-1"));

        assertThat(result.consolidatedBalance()).isEqualByComparingTo("350.50");
    }

    @Test
    void excludesArchivedAccountsFromTheTotal() {
        String archivedId = createAccount
                .execute(new CreateAccountUseCase.Input("user-1", AccountType.CASH, "Carteira", "BRL", new BigDecimal("1000.00")))
                .accountId();
        createAccount.execute(new CreateAccountUseCase.Input("user-1", AccountType.CHECKING, "Conta A", "BRL", new BigDecimal("50.00")));
        new ArchiveAccountUseCase(accountRepository).execute(new ArchiveAccountUseCase.Input("user-1", archivedId));

        GetConsolidatedBalanceUseCase.Output result = useCase.execute(new GetConsolidatedBalanceUseCase.Input("user-1"));

        assertThat(result.consolidatedBalance()).isEqualByComparingTo("50.00");
    }

    @Test
    void returnsZeroWhenTheUserHasNoActiveAccounts() {
        GetConsolidatedBalanceUseCase.Output result = useCase.execute(new GetConsolidatedBalanceUseCase.Input("user-without-accounts"));

        assertThat(result.consolidatedBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void doesNotIncludeAnotherUsersAccountsInTheTotal() {
        createAccount.execute(new CreateAccountUseCase.Input("user-1", AccountType.CHECKING, "Conta A", "BRL", new BigDecimal("100.00")));
        createAccount.execute(new CreateAccountUseCase.Input("user-2", AccountType.CHECKING, "Conta B", "BRL", new BigDecimal("999.00")));

        GetConsolidatedBalanceUseCase.Output result = useCase.execute(new GetConsolidatedBalanceUseCase.Input("user-1"));

        assertThat(result.consolidatedBalance()).isEqualByComparingTo("100.00");
    }

    @Test
    void reflectsTransactionsInTheConsolidatedTotal() {
        String accountId = createAccount
                .execute(new CreateAccountUseCase.Input("user-1", AccountType.CHECKING, "Conta A", "BRL", new BigDecimal("100.00")))
                .accountId();
        transactionRepository.save(Transaction.create(
                "tx-1", "user-1", accountId, "category-1", TransactionType.INCOME, new BigDecimal("50.00"), LocalDate.now(), null, null));
        transactionRepository.save(Transaction.create(
                "tx-2", "user-1", accountId, "category-1", TransactionType.EXPENSE, new BigDecimal("20.00"), LocalDate.now(), null, null));

        GetConsolidatedBalanceUseCase.Output result = useCase.execute(new GetConsolidatedBalanceUseCase.Input("user-1"));

        assertThat(result.consolidatedBalance()).isEqualByComparingTo("130.00");
    }
}
