package com.financepulse.engine.domain.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.financepulse.engine.domain.account.errors.InvalidCurrencyException;
import org.junit.jupiter.api.Test;

class CurrencyTest {

    @Test
    void createsAValidCurrencyNormalizedToUppercase() {
        Currency currency = Currency.create(" brl ");

        assertThat(currency.toString()).isEqualTo("BRL");
    }

    @Test
    void rejectsACodeWithMoreThanThreeLetters() {
        assertThatThrownBy(() -> Currency.create("BRLL")).isInstanceOf(InvalidCurrencyException.class);
    }

    @Test
    void rejectsACodeWithDigits() {
        assertThatThrownBy(() -> Currency.create("BR1")).isInstanceOf(InvalidCurrencyException.class);
    }

    @Test
    void rejectsAnEmptyValue() {
        assertThatThrownBy(() -> Currency.create("")).isInstanceOf(InvalidCurrencyException.class);
    }

    @Test
    void considersTwoCurrenciesWithDifferentCasingEqual() {
        assertThat(Currency.create("brl")).isEqualTo(Currency.create("BRL"));
    }
}
