package com.financepulse.engine.application.ports;

public interface PasswordHasher {

    String hash(String plainPassword);

    boolean matches(String plainPassword, String passwordHash);
}
