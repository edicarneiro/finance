package com.financepulse.engine.adapters.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.financepulse.engine.adapters.in.web.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Aplica RF-008 a rotas protegidas — equivalente ao requireAuth.ts do backend
 * TypeScript. Um HandlerInterceptor comum do Spring MVC é suficiente (ADR-0013
 * evita deliberadamente spring-boot-starter-security, que ativaria
 * autenticação automática incompatível com o JWT já desenhado).
 */
public class AuthenticationInterceptor implements HandlerInterceptor {

    public static final String USER_ID_ATTRIBUTE = "userId";

    private final AuthenticatedUserResolver authenticatedUserResolver;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AuthenticationInterceptor(AuthenticatedUserResolver authenticatedUserResolver) {
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // Preflight de CORS (ver ADR-0025): o navegador nunca envia Authorization em um OPTIONS de
        // preflight, então bloqueá-lo aqui quebraria o handshake de CORS para toda rota protegida —
        // a requisição real subsequente (com Authorization) continua sendo verificada normalmente.
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        Optional<String> userId = authenticatedUserResolver.resolve(request.getHeader("Authorization"));

        if (userId.isEmpty()) {
            writeUnauthorized(response);
            return false;
        }

        request.setAttribute(USER_ID_ATTRIBUTE, userId.get());
        return true;
    }

    private void writeUnauthorized(HttpServletResponse response) throws java.io.IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter()
                .write(objectMapper.writeValueAsString(new ErrorResponse("Token de autenticação ausente ou inválido.")));
    }

    /** Lê o userId estabelecido por este interceptor. Lançar aqui sinaliza um bug de fiação, não um erro de cliente. */
    public static String getAuthenticatedUserId(HttpServletRequest request) {
        Object userId = request.getAttribute(USER_ID_ATTRIBUTE);
        if (!(userId instanceof String)) {
            throw new IllegalStateException(
                    "getAuthenticatedUserId chamado em uma requisição que não passou por AuthenticationInterceptor.");
        }
        return (String) userId;
    }
}
