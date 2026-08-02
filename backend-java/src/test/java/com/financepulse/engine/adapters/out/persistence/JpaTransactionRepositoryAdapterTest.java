package com.financepulse.engine.adapters.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.financepulse.engine.domain.transaction.Transaction;
import com.financepulse.engine.domain.transaction.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

/** Exercita o adaptador contra H2 real, mesma disciplina dos demais adaptadores de persistência (rules.md § 3). */
@DataJpaTest
class JpaTransactionRepositoryAdapterTest {

    @Autowired
    private SpringDataTransactionJpaRepository jpaRepository;

    @Test
    void savesAndReloadsATransactionWithTags() {
        JpaTransactionRepositoryAdapter adapter = new JpaTransactionRepositoryAdapter(jpaRepository);
        Transaction transaction = Transaction.create(
                "tx-1",
                "user-1",
                "account-1",
                "category-1",
                TransactionType.EXPENSE,
                new BigDecimal("49.90"),
                LocalDate.of(2026, 7, 1),
                "Supermercado",
                List.of("essencial", "mensal"));

        adapter.save(transaction);

        Optional<Transaction> found = adapter.findByIdAndUserId("tx-1", "user-1");
        assertThat(found).isPresent();
        assertThat(found.get().getAmount()).isEqualByComparingTo("49.90");
        assertThat(found.get().getTags()).containsExactlyInAnyOrder("essencial", "mensal");
    }

    @Test
    void doesNotFindATransactionBelongingToAnotherUser() {
        JpaTransactionRepositoryAdapter adapter = new JpaTransactionRepositoryAdapter(jpaRepository);
        adapter.save(Transaction.create(
                "tx-1", "user-1", "account-1", "category-1", TransactionType.EXPENSE, BigDecimal.TEN, LocalDate.now(), null, List.of()));

        assertThat(adapter.findByIdAndUserId("tx-1", "another-user")).isEmpty();
    }

    @Test
    void listsAllTransactionsForAnAccount() {
        JpaTransactionRepositoryAdapter adapter = new JpaTransactionRepositoryAdapter(jpaRepository);
        adapter.save(Transaction.create(
                "tx-1", "user-1", "account-1", "category-1", TransactionType.EXPENSE, BigDecimal.TEN, LocalDate.now(), null, List.of()));
        adapter.save(Transaction.create(
                "tx-2", "user-1", "account-1", "category-1", TransactionType.INCOME, BigDecimal.ONE, LocalDate.now(), null, List.of()));
        adapter.save(Transaction.create(
                "tx-3", "user-1", "account-2", "category-1", TransactionType.INCOME, BigDecimal.ONE, LocalDate.now(), null, List.of()));

        List<Transaction> transactions = adapter.findAllByAccountIdAndUserId("account-1", "user-1");

        assertThat(transactions).hasSize(2).extracting(Transaction::getId).containsExactlyInAnyOrder("tx-1", "tx-2");
    }

    @Test
    void listsAllTransactionsForAUserAcrossDifferentAccounts() {
        JpaTransactionRepositoryAdapter adapter = new JpaTransactionRepositoryAdapter(jpaRepository);
        adapter.save(Transaction.create(
                "tx-1", "user-1", "account-1", "category-1", TransactionType.EXPENSE, BigDecimal.TEN, LocalDate.now(), null, List.of()));
        adapter.save(Transaction.create(
                "tx-2", "user-1", "account-2", "category-1", TransactionType.INCOME, BigDecimal.ONE, LocalDate.now(), null, List.of()));
        adapter.save(Transaction.create(
                "tx-3", "another-user", "account-3", "category-1", TransactionType.INCOME, BigDecimal.ONE, LocalDate.now(), null, List.of()));

        List<Transaction> transactions = adapter.findAllByUserId("user-1");

        assertThat(transactions).hasSize(2).extracting(Transaction::getId).containsExactlyInAnyOrder("tx-1", "tx-2");
    }

    @Test
    void persistsAnUpdateToAnExistingTransaction() {
        JpaTransactionRepositoryAdapter adapter = new JpaTransactionRepositoryAdapter(jpaRepository);
        Transaction transaction = Transaction.create(
                "tx-1", "user-1", "account-1", "category-1", TransactionType.EXPENSE, BigDecimal.TEN, LocalDate.now(), "original", List.of());
        adapter.save(transaction);

        adapter.update(transaction.withDetails(
                "account-1", "category-1", TransactionType.INCOME, new BigDecimal("20.00"), LocalDate.now(), "editado", List.of("nova")));

        Transaction reloaded = adapter.findByIdAndUserId("tx-1", "user-1").orElseThrow();
        assertThat(reloaded.getType()).isEqualTo(TransactionType.INCOME);
        assertThat(reloaded.getDescription()).isEqualTo("editado");
        assertThat(reloaded.getTags()).containsExactly("nova");
    }

    @Test
    void deletesATransactionScopedToItsOwner() {
        JpaTransactionRepositoryAdapter adapter = new JpaTransactionRepositoryAdapter(jpaRepository);
        adapter.save(Transaction.create(
                "tx-1", "user-1", "account-1", "category-1", TransactionType.EXPENSE, BigDecimal.TEN, LocalDate.now(), null, List.of()));

        adapter.deleteByIdAndUserId("tx-1", "another-user");
        assertThat(adapter.findByIdAndUserId("tx-1", "user-1"))
                .as("delete de outro usuário não deve remover a transação")
                .isPresent();

        adapter.deleteByIdAndUserId("tx-1", "user-1");
        assertThat(adapter.findByIdAndUserId("tx-1", "user-1")).isEmpty();
    }

    @Test
    void detectsWhetherACategoryHasTransactions() {
        JpaTransactionRepositoryAdapter adapter = new JpaTransactionRepositoryAdapter(jpaRepository);
        adapter.save(Transaction.create(
                "tx-1", "user-1", "account-1", "category-1", TransactionType.EXPENSE, BigDecimal.TEN, LocalDate.now(), null, List.of()));

        assertThat(adapter.existsByCategoryIdAndUserId("category-1", "user-1")).isTrue();
        assertThat(adapter.existsByCategoryIdAndUserId("category-2", "user-1")).isFalse();
        assertThat(adapter.existsByCategoryIdAndUserId("category-1", "another-user")).isFalse();
    }
}
