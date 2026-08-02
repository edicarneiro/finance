package com.financepulse.engine.composition;

import com.financepulse.engine.adapters.in.web.AuthenticatedUserResolver;
import com.financepulse.engine.adapters.in.web.AuthenticationInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Aplica {@link AuthenticationInterceptor} às rotas protegidas (ver ADR-0014)
 * e habilita CORS para as origens do frontend (ver ADR-0025) — pré-requisito
 * bloqueante para qualquer chamada do frontend ao backend real, dado que são
 * origens distintas. Sem {@code allowCredentials}: a autenticação usa o
 * header {@code Authorization: Bearer}, nunca cookie.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthenticatedUserResolver authenticatedUserResolver;
    private final String[] allowedOrigins;

    public WebMvcConfig(
            AuthenticatedUserResolver authenticatedUserResolver,
            @Value("${financepulse.cors.allowed-origins:http://localhost:5173}") String allowedOrigins) {
        this.authenticatedUserResolver = authenticatedUserResolver;
        this.allowedOrigins = allowedOrigins.split(",");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AuthenticationInterceptor(authenticatedUserResolver))
                .addPathPatterns(
                        "/accounts/**", "/transactions/**", "/categories/**", "/budgets/**", "/goals/**", "/dashboard/**", "/reports/**",
                        "/notification-preferences/**", "/notifications/**", "/users/**", "/privacy/**", "/backoffice/**");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("Authorization", "Content-Type");
    }
}
