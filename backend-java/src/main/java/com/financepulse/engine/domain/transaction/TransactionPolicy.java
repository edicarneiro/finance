package com.financepulse.engine.domain.transaction;

import com.financepulse.engine.domain.transaction.errors.InvalidAmountException;
import java.math.BigDecimal;

public final class TransactionPolicy {

    private TransactionPolicy() {
    }

    public static void assertPositiveAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidAmountException();
        }
    }
}
