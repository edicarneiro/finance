package com.financepulse.engine.adapters.in.web;

import com.financepulse.engine.application.ports.TokenService;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Extrai e valida o token de sessão (RF-008) do cabeçalho Authorization.
 * Equivalente ao requireAuth.ts do backend TypeScript. Nesta fase de
 * migração não há endpoint protegido a interceptar (ADR-0013 — a rota
 * histórica GET /auth/me não foi replicada); a validação é coberta por
 * teste direto e o componente fica pronto para ser aplicado a rotas
 * protegidas nas próximas fases (M2.1 em diante).
 */
@Component
public class AuthenticatedUserResolver {

    private static final String BEARER_PREFIX = "Bearer ";

    private final TokenService tokenService;

    public AuthenticatedUserResolver(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    public Optional<String> resolve(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            return Optional.empty();
        }

        String token = authorizationHeader.substring(BEARER_PREFIX.length());
        return tokenService.verify(token);
    }
}
