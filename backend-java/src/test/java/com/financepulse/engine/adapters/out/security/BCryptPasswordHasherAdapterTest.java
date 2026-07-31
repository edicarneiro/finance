package com.financepulse.engine.adapters.out.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BCryptPasswordHasherAdapterTest {

    private BCryptPasswordHasherAdapter hasher;

    @BeforeEach
    void setUp() {
        hasher = new BCryptPasswordHasherAdapter();
    }

    @Test
    void hashesAPasswordIntoADifferentValue() {
        String hash = hasher.hash("StrongPass1");

        assertThat(hash).isNotEqualTo("StrongPass1");
    }

    @Test
    void matchesThePlainPasswordAgainstItsOwnHash() {
        String hash = hasher.hash("StrongPass1");

        assertThat(hasher.matches("StrongPass1", hash)).isTrue();
    }

    @Test
    void rejectsAWrongPasswordAgainstAHash() {
        String hash = hasher.hash("StrongPass1");

        assertThat(hasher.matches("WrongPassword", hash)).isFalse();
    }
}
