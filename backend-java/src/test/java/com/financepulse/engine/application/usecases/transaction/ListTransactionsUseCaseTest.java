package com.financepulse.engine.application.usecases.transaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.financepulse.engine.application.usecases.account.CreateAccountUseCase;
import com.financepulse.engine.domain.account.AccountType;
import com.financepulse.engine.domain.account.errors.AccountNotFoundException;
import com.financepulse.engine.domain.category.Category;
import com.financepulse.engine.domain.transaction.TransactionType;
import com.financepulse.engine.testsupport.InMemoryAccountRepository;
import com.financepulse.engine.testsupport.InMemoryCategoryRepository;
import com.financepulse.engine.testsupport.InMemoryTransactionRepository;
import com.financepulse.engine.testsupport.SequentialIdGenerator;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ListTransactionsUseCaseTest {

    private InMemoryAccountRepository accountRepository;
    private InMemoryTransactionRepository transactionRepository;
    private CreateTransactionUseCase createTransaction;
    private ListTransactionsUseCase useCase;
    private String accountId;
    private String categoryId;

    @BeforeEach
    void setUp() {
        accountRepository = new InMemoryAccountRepository();
        InMemoryCategoryRepository categoryRepository = new InMemoryCategoryRepository();
        transactionRepository = new InMemoryTransactionRepository();
        createTransaction = new CreateTransactionUseCase(transactionRepository, accountRepository, categoryRepository, new SequentialIdGenerator("tx"));
        useCase = new ListTransactionsUseCase(transactionRepository, accountRepository);

        accountId = new CreateAccountUseCase(accountRepository, new SequentialIdGenerator("account"))
                .execute(new CreateAccountUseCase.Input("user-1", AccountType.CHECKING, "Conta A", "BRL", BigDecimal.ZERO))
                .accountId();
        Category category = Category.create("category-1", "user-1", "Alimentação", null);
        categoryRepository.save(category);
        categoryId = category.getId();
    }

    @Test
    void listsTransactionsForTheGivenAccount() {
        createTransaction.execute(new CreateTransactionUseCase.Input(
                "user-1", accountId, categoryId, TransactionType.EXPENSE, BigDecimal.TEN, LocalDate.now(), "compra", List.of()));

        ListTransactionsUseCase.Output result = useCase.execute(new ListTransactionsUseCase.Input("user-1", accountId));

        assertThat(result.transactions()).hasSize(1);
        assertThat(result.transactions().get(0).description()).isEqualTo("compra");
    }

    @Test
    void rejectsListingForANonExistentAccount() {
        assertThatThrownBy(() -> useCase.execute(new ListTransactionsUseCase.Input("user-1", "ghost-account")))
                .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    void rejectsListingForAnotherUsersAccount() {
        assertThatThrownBy(() -> useCase.execute(new ListTransactionsUseCase.Input("another-user", accountId)))
                .isInstanceOf(AccountNotFoundException.class);
    }
}
