package com.financepulse.engine.domain.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.financepulse.engine.domain.account.errors.InvalidAccountNameException;
import org.junit.jupiter.api.Test;

class AccountPolicyTest {

    @Test
    void trimsAValidName() {
        assertThat(AccountPolicy.assertValidName("  Conta Corrente  ")).isEqualTo("Conta Corrente");
    }

    @Test
    void rejectsAnEmptyName() {
        assertThatThrownBy(() -> AccountPolicy.assertValidName("   ")).isInstanceOf(InvalidAccountNameException.class);
    }

    @Test
    void rejectsANullName() {
        assertThatThrownBy(() -> AccountPolicy.assertValidName(null)).isInstanceOf(InvalidAccountNameException.class);
    }

    @Test
    void rejectsANameLongerThanTheMaximum() {
        String tooLong = "a".repeat(AccountPolicy.MAX_NAME_LENGTH + 1);

        assertThatThrownBy(() -> AccountPolicy.assertValidName(tooLong)).isInstanceOf(InvalidAccountNameException.class);
    }

    @Test
    void acceptsANameAtTheMaximumLength() {
        String maxLength = "a".repeat(AccountPolicy.MAX_NAME_LENGTH);

        assertThat(AccountPolicy.assertValidName(maxLength)).hasSize(AccountPolicy.MAX_NAME_LENGTH);
    }
}
