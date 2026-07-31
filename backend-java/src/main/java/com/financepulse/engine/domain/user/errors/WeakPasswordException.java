package com.financepulse.engine.domain.user.errors;

import com.financepulse.engine.domain.user.PasswordPolicy;

public class WeakPasswordException extends RuntimeException {

    public WeakPasswordException() {
        super("A senha deve ter pelo menos " + PasswordPolicy.MIN_PASSWORD_LENGTH + " caracteres.");
    }
}
