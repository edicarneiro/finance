package com.financepulse.engine.domain.account;

import com.financepulse.engine.domain.account.errors.InvalidAccountNameException;

public final class AccountPolicy {

    public static final int MAX_NAME_LENGTH = 100;

    private AccountPolicy() {
    }

    public static String assertValidName(String rawName) {
        String trimmed = rawName == null ? "" : rawName.trim();

        if (trimmed.isEmpty() || trimmed.length() > MAX_NAME_LENGTH) {
            throw new InvalidAccountNameException();
        }

        return trimmed;
    }
}
