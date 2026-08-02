package com.financepulse.engine.adapters.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.financepulse.engine.testsupport.FakeTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class AuthenticationInterceptorTest {

    private AuthenticationInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new AuthenticationInterceptor(new AuthenticatedUserResolver(new FakeTokenService()));
    }

    @Test
    void allowsTheRequestAndExposesTheUserIdWhenTheTokenIsValid() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token-for-user-1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean proceed = interceptor.preHandle(request, response, new Object());

        assertThat(proceed).isTrue();
        assertThat(AuthenticationInterceptor.getAuthenticatedUserId(request)).isEqualTo("user-1");
    }

    @Test
    void blocksTheRequestWithA401WhenTheHeaderIsMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean proceed = interceptor.preHandle(request, response, new Object());

        assertThat(proceed).isFalse();
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("Token de autenticação");
    }

    @Test
    void blocksTheRequestWithA401WhenTheTokenIsInvalid() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer not-a-real-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean proceed = interceptor.preHandle(request, response, new Object());

        assertThat(proceed).isFalse();
        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    void allowsAnOptionsRequestWithoutAnAuthorizationHeaderSoCorsPreflightIsNeverBlocked() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("OPTIONS");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean proceed = interceptor.preHandle(request, response, new Object());

        assertThat(proceed).isTrue();
        assertThat(response.getStatus()).isNotEqualTo(401);
    }

    @Test
    void gettingTheUserIdWithoutHavingGoneThroughTheInterceptorThrows() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalStateException.class, () -> AuthenticationInterceptor.getAuthenticatedUserId(request));
    }
}
