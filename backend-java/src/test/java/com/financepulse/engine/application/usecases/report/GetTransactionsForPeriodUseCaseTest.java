package com.financepulse.engine.application.usecases.report;

import static org.assertj.core.api.Assertions.assertThat;

import com.financepulse.engine.domain.account.Account;
import com.financepulse.engine.domain.account.AccountType;
import com.financepulse.engine.domain.account.Currency;
import com.financepulse.engine.domain.category.Category;
import com.financepulse.engine.domain.transaction.Transaction;
import com.financepulse.engine.domain.transaction.TransactionType;
import com.financepulse.engine.testsupport.InMemoryAccountRepository;
import com.financepulse.engine.testsupport.InMemoryCategoryRepository;
import com.financepulse.engine.testsupport.InMemoryTransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GetTransactionsForPeriodUseCaseTest {

    private InMemoryTransactionRepository transactionRepository;
    private InMemoryAccountRepository accountRepository;
    private InMemoryCategoryRepository categoryRepository;
    private GetTransactionsForPeriodUseCase useCase;

    @BeforeEach
    void setUp() {
        transactionRepository = new InMemoryTransactionRepository();
        accountRepository = new InMemoryAccountRepository();
        categoryRepository = new InMemoryCategoryRepository();
        useCase = new GetTransactionsForPeriodUseCase(transactionRepository, accountRepository, categoryRepository);

        accountRepository.save(Account.create("account-1", "user-1", AccountType.CHECKING, "Conta Corrente", Currency.create("BRL"), BigDecimal.ZERO));
        categoryRepository.save(Category.create("cat-food", "user-1", "Alimentação", null));
    }

    @Test
    void returnsTransactionsInThePeriodEnrichedWithAccountAndCategoryNamesSortedByDate() {
        transactionRepository.save(Transaction.create(
                "tx-2", "user-1", "account-1", "cat-food", TransactionType.EXPENSE, new BigDecimal("50"), LocalDate.of(2026, 7, 20), "Mercado",
                List.of("essencial")));
        transactionRepository.save(Transaction.create(
                "tx-1", "user-1", "account-1", "cat-food", TransactionType.EXPENSE, new BigDecimal("30"), LocalDate.of(2026, 7, 10), null, List.of()));
        transactionRepository.save(Transaction.create(
                "tx-outside", "user-1", "account-1", "cat-food", TransactionType.EXPENSE, new BigDecimal("999"), LocalDate.of(2026, 6, 1), null,
                List.of()));

        GetTransactionsForPeriodUseCase.Output output =
                useCase.execute(new GetTransactionsForPeriodUseCase.Input("user-1", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31)));

        assertThat(output.transactions()).hasSize(2);
        assertThat(output.transactions().get(0).date()).isEqualTo(LocalDate.of(2026, 7, 10));
        assertThat(output.transactions().get(1).date()).isEqualTo(LocalDate.of(2026, 7, 20));
        assertThat(output.transactions().get(1).accountName()).isEqualTo("Conta Corrente");
        assertThat(output.transactions().get(1).categoryName()).isEqualTo("Alimentação");
        assertThat(output.transactions().get(1).tags()).containsExactly("essencial");
    }
}
