package com.financepulse.engine.domain.transaction;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.financepulse.engine.domain.transaction.errors.InvalidAmountException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class TransactionPolicyTest {

    @Test
    void acceptsAPositiveAmount() {
        assertThatCode(() -> TransactionPolicy.assertPositiveAmount(new BigDecimal("0.01"))).doesNotThrowAnyException();
    }

    @Test
    void rejectsAZeroAmount() {
        assertThatThrownBy(() -> TransactionPolicy.assertPositiveAmount(BigDecimal.ZERO))
                .isInstanceOf(InvalidAmountException.class);
    }

    @Test
    void rejectsANegativeAmount() {
        assertThatThrownBy(() -> TransactionPolicy.assertPositiveAmount(new BigDecimal("-10.00")))
                .isInstanceOf(InvalidAmountException.class);
    }

    @Test
    void rejectsANullAmount() {
        assertThatThrownBy(() -> TransactionPolicy.assertPositiveAmount(null)).isInstanceOf(InvalidAmountException.class);
    }
}
