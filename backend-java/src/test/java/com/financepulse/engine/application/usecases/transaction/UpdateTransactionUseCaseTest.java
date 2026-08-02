package com.financepulse.engine.application.usecases.transaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.financepulse.engine.application.usecases.account.ArchiveAccountUseCase;
import com.financepulse.engine.application.usecases.account.CreateAccountUseCase;
import com.financepulse.engine.domain.account.AccountType;
import com.financepulse.engine.domain.account.errors.ArchivedAccountException;
import com.financepulse.engine.domain.category.Category;
import com.financepulse.engine.domain.category.errors.CategoryNotFoundException;
import com.financepulse.engine.domain.transaction.TransactionType;
import com.financepulse.engine.domain.transaction.errors.TransactionNotFoundException;
import com.financepulse.engine.testsupport.InMemoryAccountRepository;
import com.financepulse.engine.testsupport.InMemoryCategoryRepository;
import com.financepulse.engine.testsupport.InMemoryTransactionRepository;
import com.financepulse.engine.testsupport.SequentialIdGenerator;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UpdateTransactionUseCaseTest {

    private InMemoryAccountRepository accountRepository;
    private InMemoryCategoryRepository categoryRepository;
    private InMemoryTransactionRepository transactionRepository;
    private UpdateTransactionUseCase useCase;
    private String accountId;
    private String categoryId;
    private String transactionId;

    @BeforeEach
    void setUp() {
        accountRepository = new InMemoryAccountRepository();
        categoryRepository = new InMemoryCategoryRepository();
        transactionRepository = new InMemoryTransactionRepository();
        useCase = new UpdateTransactionUseCase(transactionRepository, accountRepository, categoryRepository);

        accountId = new CreateAccountUseCase(accountRepository, new SequentialIdGenerator("account"))
                .execute(new CreateAccountUseCase.Input("user-1", AccountType.CHECKING, "Conta A", "BRL", BigDecimal.ZERO))
                .accountId();
        Category category = Category.create("category-1", "user-1", "Alimentação", null);
        categoryRepository.save(category);
        categoryId = category.getId();

        transactionId = new CreateTransactionUseCase(transactionRepository, accountRepository, categoryRepository, new SequentialIdGenerator("tx"))
                .execute(new CreateTransactionUseCase.Input(
                        "user-1", accountId, categoryId, TransactionType.EXPENSE, BigDecimal.TEN, LocalDate.now(), "original", List.of()))
                .transactionId();
    }

    @Test
    void updatesAllFieldsOfAnOwnedTransaction() {
        useCase.execute(new UpdateTransactionUseCase.Input(
                "user-1", transactionId, accountId, categoryId, TransactionType.INCOME, new BigDecimal("99.00"), LocalDate.now(), "editado", List.of("nova")));

        var updated = transactionRepository.findByIdAndUserId(transactionId, "user-1").orElseThrow();
        assertThat(updated.getType()).isEqualTo(TransactionType.INCOME);
        assertThat(updated.getAmount()).isEqualByComparingTo("99.00");
        assertThat(updated.getDescription()).isEqualTo("editado");
    }

    @Test
    void rejectsUpdatingANonExistentTransaction() {
        assertThatThrownBy(() -> useCase.execute(new UpdateTransactionUseCase.Input(
                        "user-1", "ghost-tx", accountId, categoryId, TransactionType.EXPENSE, BigDecimal.TEN, LocalDate.now(), null, List.of())))
                .isInstanceOf(TransactionNotFoundException.class);
    }

    @Test
    void rejectsUpdatingAnotherUsersTransactionWithTheSameErrorAsNotFound() {
        assertThatThrownBy(() -> useCase.execute(new UpdateTransactionUseCase.Input(
                        "another-user", transactionId, accountId, categoryId, TransactionType.EXPENSE, BigDecimal.TEN, LocalDate.now(), null, List.of())))
                .isInstanceOf(TransactionNotFoundException.class);
    }

    @Test
    void rejectsMovingToANonExistentCategory() {
        assertThatThrownBy(() -> useCase.execute(new UpdateTransactionUseCase.Input(
                        "user-1", transactionId, accountId, "ghost-category", TransactionType.EXPENSE, BigDecimal.TEN, LocalDate.now(), null, List.of())))
                .isInstanceOf(CategoryNotFoundException.class);
    }

    @Test
    void allowsCorrectingATransactionThatAlreadyBelongsToAnAccountArchivedAfterTheFact() {
        new ArchiveAccountUseCase(accountRepository).execute(new ArchiveAccountUseCase.Input("user-1", accountId));

        useCase.execute(new UpdateTransactionUseCase.Input(
                "user-1", transactionId, accountId, categoryId, TransactionType.EXPENSE, new BigDecimal("15.00"), LocalDate.now(), "corrigido", List.of()));

        var updated = transactionRepository.findByIdAndUserId(transactionId, "user-1").orElseThrow();
        assertThat(updated.getAmount()).isEqualByComparingTo("15.00");
        assertThat(updated.getDescription()).isEqualTo("corrigido");
    }

    @Test
    void rejectsMovingATransactionIntoAnArchivedAccount() {
        String otherAccountId = new CreateAccountUseCase(accountRepository, new SequentialIdGenerator("other-account"))
                .execute(new CreateAccountUseCase.Input("user-1", AccountType.SAVINGS, "Conta B", "BRL", BigDecimal.ZERO))
                .accountId();
        new ArchiveAccountUseCase(accountRepository).execute(new ArchiveAccountUseCase.Input("user-1", otherAccountId));

        assertThatThrownBy(() -> useCase.execute(new UpdateTransactionUseCase.Input(
                        "user-1", transactionId, otherAccountId, categoryId, TransactionType.EXPENSE, BigDecimal.TEN, LocalDate.now(), null, List.of())))
                .isInstanceOf(ArchivedAccountException.class);
    }
}
