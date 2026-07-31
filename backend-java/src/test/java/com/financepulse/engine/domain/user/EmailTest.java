package com.financepulse.engine.domain.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.financepulse.engine.domain.user.errors.InvalidEmailException;
import org.junit.jupiter.api.Test;

class EmailTest {

    @Test
    void createsAValidEmailNormalizedToLowercaseAndTrimmed() {
        Email email = Email.create(" User@Example.com ");

        assertThat(email.toString()).isEqualTo("user@example.com");
    }

    @Test
    void rejectsAValueWithoutAnAtSymbol() {
        assertThatThrownBy(() -> Email.create("invalid-email")).isInstanceOf(InvalidEmailException.class);
    }

    @Test
    void rejectsAValueWithoutADomain() {
        assertThatThrownBy(() -> Email.create("user@")).isInstanceOf(InvalidEmailException.class);
    }

    @Test
    void rejectsAnEmptyValue() {
        assertThatThrownBy(() -> Email.create("")).isInstanceOf(InvalidEmailException.class);
    }

    @Test
    void considersTwoEmailsWithDifferentCasingEqual() {
        Email a = Email.create("User@Example.com");
        Email b = Email.create("user@example.com");

        assertThat(a).isEqualTo(b);
    }
}
