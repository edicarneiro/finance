package com.financepulse.engine.domain.transaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.financepulse.engine.domain.transaction.errors.InvalidAmountException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class TransactionTest {

    @Test
    void createsATransactionWithTheGivenDetails() {
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

        assertThat(transaction.getId()).isEqualTo("tx-1");
        assertThat(transaction.getType()).isEqualTo(TransactionType.EXPENSE);
        assertThat(transaction.getAmount()).isEqualByComparingTo("49.90");
        assertThat(transaction.getTags()).containsExactly("essencial", "mensal");
    }

    @Test
    void treatsANullTagListAsEmpty() {
        Transaction transaction = Transaction.create(
                "tx-1", "user-1", "account-1", "category-1", TransactionType.INCOME, BigDecimal.TEN, LocalDate.now(), null, null);

        assertThat(transaction.getTags()).isEmpty();
    }

    @Test
    void rejectsCreationWithANonPositiveAmount() {
        assertThatThrownBy(() -> Transaction.create(
                        "tx-1", "user-1", "account-1", "category-1", TransactionType.EXPENSE, BigDecimal.ZERO, LocalDate.now(), null, null))
                .isInstanceOf(InvalidAmountException.class);
    }

    @Test
    void editingDetailsProducesANewInstanceWithoutChangingIdentity() {
        Transaction original = Transaction.create(
                "tx-1", "user-1", "account-1", "category-1", TransactionType.EXPENSE, BigDecimal.TEN, LocalDate.now(), "original", List.of());

        Transaction edited = original.withDetails(
                "account-2", "category-2", TransactionType.INCOME, new BigDecimal("20.00"), LocalDate.now(), "editado", List.of("nova-tag"));

        assertThat(edited.getId()).isEqualTo(original.getId());
        assertThat(edited.getUserId()).isEqualTo(original.getUserId());
        assertThat(edited.getAccountId()).isEqualTo("account-2");
        assertThat(edited.getCategoryId()).isEqualTo("category-2");
        assertThat(edited.getType()).isEqualTo(TransactionType.INCOME);
        assertThat(edited.getAmount()).isEqualByComparingTo("20.00");
        assertThat(edited.getDescription()).isEqualTo("editado");
        assertThat(edited.getTags()).containsExactly("nova-tag");
    }

    @Test
    void rejectsEditingToANonPositiveAmount() {
        Transaction original = Transaction.create(
                "tx-1", "user-1", "account-1", "category-1", TransactionType.EXPENSE, BigDecimal.TEN, LocalDate.now(), null, List.of());

        assertThatThrownBy(() -> original.withDetails(
                        "account-1", "category-1", TransactionType.EXPENSE, BigDecimal.ZERO, LocalDate.now(), null, List.of()))
                .isInstanceOf(InvalidAmountException.class);
    }
}
