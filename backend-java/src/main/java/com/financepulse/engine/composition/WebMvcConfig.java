package com.financepulse.engine.composition;

import com.financepulse.engine.adapters.in.web.AuthenticatedUserResolver;
import com.financepulse.engine.adapters.in.web.AuthenticationInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Aplica {@link AuthenticationInterceptor} às rotas protegidas (ver ADR-0014). */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthenticatedUserResolver authenticatedUserResolver;

    public WebMvcConfig(AuthenticatedUserResolver authenticatedUserResolver) {
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AuthenticationInterceptor(authenticatedUserResolver)).addPathPatterns("/accounts/**");
    }
}
