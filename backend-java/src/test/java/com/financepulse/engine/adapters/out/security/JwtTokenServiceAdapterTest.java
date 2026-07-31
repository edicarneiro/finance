package com.financepulse.engine.adapters.out.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtTokenServiceAdapterTest {

    private JwtTokenServiceAdapter tokenService;

    @BeforeEach
    void setUp() {
        tokenService = new JwtTokenServiceAdapter("test-secret");
    }

    @Test
    void issuesATokenThatResolvesBackToTheSameUserId() {
        String token = tokenService.issue("user-1");

        assertThat(tokenService.verify(token)).contains("user-1");
    }

    @Test
    void rejectsAMalformedToken() {
        assertThat(tokenService.verify("not-a-real-token")).isEqualTo(Optional.empty());
    }

    @Test
    void rejectsATokenSignedWithADifferentSecret() {
        JwtTokenServiceAdapter otherService = new JwtTokenServiceAdapter("a-different-secret");
        String token = otherService.issue("user-1");

        assertThat(tokenService.verify(token)).isEqualTo(Optional.empty());
    }
}
