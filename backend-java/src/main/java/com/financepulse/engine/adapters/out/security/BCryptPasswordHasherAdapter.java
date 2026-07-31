package com.financepulse.engine.adapters.out.security;

import com.financepulse.engine.application.ports.PasswordHasher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class BCryptPasswordHasherAdapter implements PasswordHasher {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Override
    public String hash(String plainPassword) {
        return encoder.encode(plainPassword);
    }

    @Override
    public boolean matches(String plainPassword, String passwordHash) {
        return encoder.matches(plainPassword, passwordHash);
    }
}
