package com.financepulse.engine.application.usecases.category;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.financepulse.engine.application.usecases.account.CreateAccountUseCase;
import com.financepulse.engine.application.usecases.transaction.CreateTransactionUseCase;
import com.financepulse.engine.domain.account.AccountType;
import com.financepulse.engine.domain.category.errors.CategoryHasSubcategoriesException;
import com.financepulse.engine.domain.category.errors.CategoryHasTransactionsException;
import com.financepulse.engine.domain.category.errors.CategoryNotFoundException;
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

class DeleteCategoryUseCaseTest {

    private InMemoryCategoryRepository categoryRepository;
    private InMemoryTransactionRepository transactionRepository;
    private CreateCategoryUseCase createCategory;
    private DeleteCategoryUseCase useCase;
    private String categoryId;

    @BeforeEach
    void setUp() {
        categoryRepository = new InMemoryCategoryRepository();
        transactionRepository = new InMemoryTransactionRepository();
        createCategory = new CreateCategoryUseCase(categoryRepository, new SequentialIdGenerator("category"));
        useCase = new DeleteCategoryUseCase(categoryRepository, transactionRepository);

        categoryId = createCategory.execute(new CreateCategoryUseCase.Input("user-1", "Alimentação", null)).categoryId();
    }

    @Test
    void deletesACategoryWithNoSubcategoriesOrTransactions() {
        useCase.execute(new DeleteCategoryUseCase.Input("user-1", categoryId));

        assertThat(categoryRepository.findByIdAndUserId(categoryId, "user-1")).isEmpty();
    }

    @Test
    void rejectsDeletingACategoryThatHasSubcategories() {
        createCategory.execute(new CreateCategoryUseCase.Input("user-1", "Restaurante", categoryId));

        assertThatThrownBy(() -> useCase.execute(new DeleteCategoryUseCase.Input("user-1", categoryId)))
                .isInstanceOf(CategoryHasSubcategoriesException.class);
    }

    @Test
    void rejectsDeletingACategoryThatHasTransactions() {
        InMemoryAccountRepository accountRepository = new InMemoryAccountRepository();
        String accountId = new CreateAccountUseCase(accountRepository, new SequentialIdGenerator("account"))
                .execute(new CreateAccountUseCase.Input("user-1", AccountType.CHECKING, "Conta A", "BRL", BigDecimal.ZERO))
                .accountId();
        new CreateTransactionUseCase(transactionRepository, accountRepository, categoryRepository, new SequentialIdGenerator("tx"))
                .execute(new CreateTransactionUseCase.Input(
                        "user-1", accountId, categoryId, TransactionType.EXPENSE, BigDecimal.TEN, LocalDate.now(), null, List.of()));

        assertThatThrownBy(() -> useCase.execute(new DeleteCategoryUseCase.Input("user-1", categoryId)))
                .isInstanceOf(CategoryHasTransactionsException.class);
    }

    @Test
    void rejectsDeletingANonExistentCategory() {
        assertThatThrownBy(() -> useCase.execute(new DeleteCategoryUseCase.Input("user-1", "ghost-category")))
                .isInstanceOf(CategoryNotFoundException.class);
    }

    @Test
    void rejectsDeletingAnotherUsersCategoryWithTheSameErrorAsNotFound() {
        assertThatThrownBy(() -> useCase.execute(new DeleteCategoryUseCase.Input("another-user", categoryId)))
                .isInstanceOf(CategoryNotFoundException.class);
    }
}
