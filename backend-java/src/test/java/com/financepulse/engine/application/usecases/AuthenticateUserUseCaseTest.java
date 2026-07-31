package com.financepulse.engine.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.financepulse.engine.domain.user.errors.InvalidCredentialsException;
import com.financepulse.engine.testsupport.FakePasswordHasher;
import com.financepulse.engine.testsupport.FakeTokenService;
import com.financepulse.engine.testsupport.InMemoryUserRepository;
import com.financepulse.engine.testsupport.SequentialIdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AuthenticateUserUseCaseTest {

    private InMemoryUserRepository userRepository;
    private FakePasswordHasher passwordHasher;
    private AuthenticateUserUseCase useCase;

    @BeforeEach
    void setUp() {
        userRepository = new InMemoryUserRepository();
        passwordHasher = new FakePasswordHasher();
        useCase = new AuthenticateUserUseCase(userRepository, passwordHasher, new FakeTokenService());

        RegisterUserUseCase registerUser = new RegisterUserUseCase(userRepository, passwordHasher, new SequentialIdGenerator("user"));
        registerUser.execute(new RegisterUserUseCase.Input("user@example.com", "StrongPass1"));
    }

    @Test
    void issuesAnAccessTokenForValidCredentials() {
        AuthenticateUserUseCase.Output result = useCase.execute(new AuthenticateUserUseCase.Input("user@example.com", "StrongPass1"));

        assertThat(result.token()).isEqualTo("token-for-user-1");
    }

    @Test
    void rejectsAuthenticationForANonExistentEmail() {
        assertThatThrownBy(() -> useCase.execute(new AuthenticateUserUseCase.Input("ghost@example.com", "whatever1")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void rejectsAuthenticationWithAWrongPasswordUsingTheSameErrorAsAnUnknownEmail() {
        // Mesmo tipo de erro para "e-mail não encontrado" e "senha incorreta" de propósito:
        // impede que o endpoint de login seja usado para enumerar e-mails cadastrados.
        assertThatThrownBy(() -> useCase.execute(new AuthenticateUserUseCase.Input("user@example.com", "WrongPass1")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void isCaseInsensitiveOnEmailLookup() {
        AuthenticateUserUseCase.Output result = useCase.execute(new AuthenticateUserUseCase.Input("USER@example.com", "StrongPass1"));

        assertThat(result.token()).isEqualTo("token-for-user-1");
    }
}
