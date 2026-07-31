package com.financepulse.engine.domain.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.financepulse.engine.domain.account.errors.InvalidAccountNameException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class AccountTest {

    @Test
    void createsAnAccountWithTheInitialBalanceAsCurrentBalance() {
        Account account = Account.create(
                "account-1", "user-1", AccountType.CHECKING, "Conta Corrente", Currency.create("BRL"), new BigDecimal("100.00"));

        assertThat(account.getId()).isEqualTo("account-1");
        assertThat(account.getUserId()).isEqualTo("user-1");
        assertThat(account.getType()).isEqualTo(AccountType.CHECKING);
        assertThat(account.getName()).isEqualTo("Conta Corrente");
        assertThat(account.getCurrency()).isEqualTo(Currency.create("BRL"));
        assertThat(account.getBalance()).isEqualByComparingTo("100.00");
        assertThat(account.isArchived()).isFalse();
    }

    @Test
    void acceptsANegativeInitialBalanceForCreditCardTypeAccounts() {
        Account account = Account.create(
                "account-1", "user-1", AccountType.CREDIT_CARD, "Cartão", Currency.create("BRL"), new BigDecimal("-500.00"));

        assertThat(account.getBalance()).isEqualByComparingTo("-500.00");
    }

    @Test
    void rejectsCreationWithAnInvalidName() {
        assertThatThrownBy(() ->
                        Account.create("account-1", "user-1", AccountType.CASH, "", Currency.create("BRL"), BigDecimal.ZERO))
                .isInstanceOf(InvalidAccountNameException.class);
    }

    @Test
    void editingTheNameProducesANewInstanceWithoutChangingOtherFields() {
        Account original = Account.create(
                "account-1", "user-1", AccountType.SAVINGS, "Poupança", Currency.create("BRL"), new BigDecimal("50.00"));

        Account renamed = original.withName("Poupança Itaú");

        assertThat(renamed.getName()).isEqualTo("Poupança Itaú");
        assertThat(renamed.getId()).isEqualTo(original.getId());
        assertThat(renamed.getType()).isEqualTo(original.getType());
        assertThat(renamed.getBalance()).isEqualByComparingTo(original.getBalance());
        assertThat(renamed.isArchived()).isFalse();
    }

    @Test
    void rejectsRenamingToAnInvalidName() {
        Account account = Account.create(
                "account-1", "user-1", AccountType.CASH, "Carteira", Currency.create("BRL"), BigDecimal.ZERO);

        assertThatThrownBy(() -> account.withName("   ")).isInstanceOf(InvalidAccountNameException.class);
    }

    @Test
    void archivingAnActiveAccountMarksItAsArchived() {
        Account account = Account.create(
                "account-1", "user-1", AccountType.CASH, "Carteira", Currency.create("BRL"), BigDecimal.ZERO);

        Account archived = account.archive();

        assertThat(archived.isArchived()).isTrue();
        assertThat(account.isArchived()).isFalse();
    }

    @Test
    void archivingAnAlreadyArchivedAccountIsIdempotent() {
        Account archived = Account.create(
                        "account-1", "user-1", AccountType.CASH, "Carteira", Currency.create("BRL"), BigDecimal.ZERO)
                .archive();

        Account archivedAgain = archived.archive();

        assertThat(archivedAgain.isArchived()).isTrue();
        assertThat(archivedAgain).isSameAs(archived);
    }
}
