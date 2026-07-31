package com.financepulse.engine.testsupport;

import com.financepulse.engine.application.ports.PasswordHasher;

public class FakePasswordHasher implements PasswordHasher {

    @Override
    public String hash(String plainPassword) {
        return "hashed:" + plainPassword;
    }

    @Override
    public boolean matches(String plainPassword, String passwordHash) {
        return passwordHash.equals("hashed:" + plainPassword);
    }
}
