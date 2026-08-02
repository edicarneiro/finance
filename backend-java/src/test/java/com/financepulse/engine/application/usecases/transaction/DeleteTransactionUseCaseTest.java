package com.financepulse.engine.application.usecases.transaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.financepulse.engine.application.usecases.account.CreateAccountUseCase;
import com.financepulse.engine.domain.account.AccountType;
import com.financepulse.engine.domain.category.Category;
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

class DeleteTransactionUseCaseTest {

    private InMemoryTransactionRepository transactionRepository;
    private DeleteTransactionUseCase useCase;
    private String transactionId;

    @BeforeEach
    void setUp() {
        InMemoryAccountRepository accountRepository = new InMemoryAccountRepository();
        InMemoryCategoryRepository categoryRepository = new InMemoryCategoryRepository();
        transactionRepository = new InMemoryTransactionRepository();
        useCase = new DeleteTransactionUseCase(transactionRepository);

        String accountId = new CreateAccountUseCase(accountRepository, new SequentialIdGenerator("account"))
                .execute(new CreateAccountUseCase.Input("user-1", AccountType.CHECKING, "Conta A", "BRL", BigDecimal.ZERO))
                .accountId();
        Category category = Category.create("category-1", "user-1", "Alimentação", null);
        categoryRepository.save(category);

        transactionId = new CreateTransactionUseCase(transactionRepository, accountRepository, categoryRepository, new SequentialIdGenerator("tx"))
                .execute(new CreateTransactionUseCase.Input(
                        "user-1", accountId, category.getId(), TransactionType.EXPENSE, BigDecimal.TEN, LocalDate.now(), null, List.of()))
                .transactionId();
    }

    @Test
    void deletesAnOwnedTransaction() {
        useCase.execute(new DeleteTransactionUseCase.Input("user-1", transactionId));

        assertThat(transactionRepository.findByIdAndUserId(transactionId, "user-1")).isEmpty();
    }

    @Test
    void rejectsDeletingANonExistentTransaction() {
        assertThatThrownBy(() -> useCase.execute(new DeleteTransactionUseCase.Input("user-1", "ghost-tx")))
                .isInstanceOf(TransactionNotFoundException.class);
    }

    @Test
    void rejectsDeletingAnotherUsersTransactionWithTheSameErrorAsNotFound() {
        assertThatThrownBy(() -> useCase.execute(new DeleteTransactionUseCase.Input("another-user", transactionId)))
                .isInstanceOf(TransactionNotFoundException.class);

        assertThat(transactionRepository.findByIdAndUserId(transactionId, "user-1"))
                .as("a transação não deve ter sido excluída pela tentativa de outro usuário")
                .isPresent();
    }
}
