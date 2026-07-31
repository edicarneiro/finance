package com.financepulse.engine.domain.user;

import com.financepulse.engine.domain.user.errors.WeakPasswordException;

public final class PasswordPolicy {

    public static final int MIN_PASSWORD_LENGTH = 8;

    private PasswordPolicy() {
    }

    public static void assertStrongPassword(String plainPassword) {
        if (plainPassword == null || plainPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new WeakPasswordException();
        }
    }
}
