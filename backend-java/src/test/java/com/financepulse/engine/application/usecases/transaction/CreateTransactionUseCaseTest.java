package com.financepulse.engine.application.usecases.transaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.financepulse.engine.application.usecases.account.ArchiveAccountUseCase;
import com.financepulse.engine.application.usecases.account.CreateAccountUseCase;
import com.financepulse.engine.domain.account.AccountType;
import com.financepulse.engine.domain.account.errors.AccountNotFoundException;
import com.financepulse.engine.domain.account.errors.ArchivedAccountException;
import com.financepulse.engine.domain.category.Category;
import com.financepulse.engine.domain.category.errors.CategoryNotFoundException;
import com.financepulse.engine.domain.transaction.TransactionType;
import com.financepulse.engine.domain.transaction.errors.InvalidAmountException;
import com.financepulse.engine.testsupport.InMemoryAccountRepository;
import com.financepulse.engine.testsupport.InMemoryCategoryRepository;
import com.financepulse.engine.testsupport.InMemoryTransactionRepository;
import com.financepulse.engine.testsupport.SequentialIdGenerator;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CreateTransactionUseCaseTest {

    private InMemoryAccountRepository accountRepository;
    private InMemoryCategoryRepository categoryRepository;
    private InMemoryTransactionRepository transactionRepository;
    private CreateTransactionUseCase useCase;
    private String accountId;
    private String categoryId;

    @BeforeEach
    void setUp() {
        accountRepository = new InMemoryAccountRepository();
        categoryRepository = new InMemoryCategoryRepository();
        transactionRepository = new InMemoryTransactionRepository();
        useCase = new CreateTransactionUseCase(
                transactionRepository, accountRepository, categoryRepository, new SequentialIdGenerator("tx"));

        accountId = new CreateAccountUseCase(accountRepository, new SequentialIdGenerator("account"))
                .execute(new CreateAccountUseCase.Input("user-1", AccountType.CHECKING, "Conta A", "BRL", BigDecimal.ZERO))
                .accountId();
        Category category = Category.create("category-1", "user-1", "Alimentação", null);
        categoryRepository.save(category);
        categoryId = category.getId();
    }

    @Test
    void createsATransactionForAnOwnedAccountAndCategory() {
        CreateTransactionUseCase.Output result = useCase.execute(new CreateTransactionUseCase.Input(
                "user-1", accountId, categoryId, TransactionType.EXPENSE, new BigDecimal("49.90"), LocalDate.now(), "Mercado", List.of("essencial")));

        assertThat(result.transactionId()).isEqualTo("tx-1");
        assertThat(transactionRepository.findByIdAndUserId("tx-1", "user-1")).isPresent();
    }

    @Test
    void rejectsANonExistentAccount() {
        assertThatThrownBy(() -> useCase.execute(new CreateTransactionUseCase.Input(
                        "user-1", "ghost-account", categoryId, TransactionType.EXPENSE, BigDecimal.TEN, LocalDate.now(), null, null)))
                .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    void rejectsAnAccountBelongingToAnotherUser() {
        assertThatThrownBy(() -> useCase.execute(new CreateTransactionUseCase.Input(
                        "another-user", accountId, categoryId, TransactionType.EXPENSE, BigDecimal.TEN, LocalDate.now(), null, null)))
                .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    void rejectsAnArchivedAccount() {
        new ArchiveAccountUseCase(accountRepository).execute(new ArchiveAccountUseCase.Input("user-1", accountId));

        assertThatThrownBy(() -> useCase.execute(new CreateTransactionUseCase.Input(
                        "user-1", accountId, categoryId, TransactionType.EXPENSE, BigDecimal.TEN, LocalDate.now(), null, null)))
                .isInstanceOf(ArchivedAccountException.class);
    }

    @Test
    void rejectsANonExistentCategory() {
        assertThatThrownBy(() -> useCase.execute(new CreateTransactionUseCase.Input(
                        "user-1", accountId, "ghost-category", TransactionType.EXPENSE, BigDecimal.TEN, LocalDate.now(), null, null)))
                .isInstanceOf(CategoryNotFoundException.class);
    }

    @Test
    void rejectsANonPositiveAmount() {
        assertThatThrownBy(() -> useCase.execute(new CreateTransactionUseCase.Input(
                        "user-1", accountId, categoryId, TransactionType.EXPENSE, BigDecimal.ZERO, LocalDate.now(), null, null)))
                .isInstanceOf(InvalidAmountException.class);
    }
}
