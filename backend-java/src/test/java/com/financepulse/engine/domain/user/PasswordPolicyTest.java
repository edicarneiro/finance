package com.financepulse.engine.domain.user;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.financepulse.engine.domain.user.errors.WeakPasswordException;
import org.junit.jupiter.api.Test;

class PasswordPolicyTest {

    @Test
    void acceptsAPasswordAtTheMinimumLength() {
        assertThatCode(() -> PasswordPolicy.assertStrongPassword("12345678")).doesNotThrowAnyException();
    }

    @Test
    void rejectsAPasswordShorterThanTheMinimumLength() {
        assertThatThrownBy(() -> PasswordPolicy.assertStrongPassword("short"))
                .isInstanceOf(WeakPasswordException.class);
    }

    @Test
    void rejectsANullPassword() {
        assertThatThrownBy(() -> PasswordPolicy.assertStrongPassword(null))
                .isInstanceOf(WeakPasswordException.class);
    }
}
