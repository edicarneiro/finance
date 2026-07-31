package com.financepulse.engine.adapters.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.financepulse.engine.testsupport.FakeTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Valida a emissão/verificação de token de sessão (RF-008) diretamente,
 * sem um endpoint de negócio protegido — decisão de escopo registrada em
 * ADR-0013 (a rota histórica GET /auth/me não foi replicada nesta fase).
 */
class AuthenticatedUserResolverTest {

    private AuthenticatedUserResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new AuthenticatedUserResolver(new FakeTokenService());
    }

    @Test
    void resolvesTheUserIdFromAValidBearerToken() {
        assertThat(resolver.resolve("Bearer token-for-user-1")).contains("user-1");
    }

    @Test
    void returnsEmptyWhenTheHeaderIsMissing() {
        assertThat(resolver.resolve(null)).isEmpty();
    }

    @Test
    void returnsEmptyWhenTheHeaderDoesNotUseTheBearerScheme() {
        assertThat(resolver.resolve("token-for-user-1")).isEmpty();
    }

    @Test
    void returnsEmptyWhenTheTokenIsInvalid() {
        assertThat(resolver.resolve("Bearer not-a-real-token")).isEmpty();
    }
}
