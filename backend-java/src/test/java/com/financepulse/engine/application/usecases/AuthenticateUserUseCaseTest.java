package com.financepulse.engine.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.financepulse.engine.domain.user.Email;
import com.financepulse.engine.domain.user.Role;
import com.financepulse.engine.domain.user.User;
import com.financepulse.engine.domain.user.errors.AccountSuspendedException;
import com.financepulse.engine.domain.user.errors.InvalidCredentialsException;
import com.financepulse.engine.testsupport.FakePasswordHasher;
import com.financepulse.engine.testsupport.FakeTokenService;
import com.financepulse.engine.testsupport.InMemoryUserRepository;
import com.financepulse.engine.testsupport.SequentialIdGenerator;
import java.time.Instant;
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

    @Test
    void rejectsAuthenticationForADeletedUserEvenIfThePasswordHashStillMatched() {
        // Isola especificamente a checagem de isDeleted() (defesa em profundidade, ver ADR-0023): o hash aqui
        // é montado para bater com a senha informada, provando que é o deletedAt (não a senha) que bloqueia.
        User user = userRepository.findByEmail(Email.create("user@example.com")).orElseThrow();
        User deletedUser = User.reconstitute(
                user.getId(), user.getEmail(), "hashed:StrongPass1", user.getName(), user.getCreatedAt(), Instant.now(), Role.CUSTOMER, null);
        userRepository.update(deletedUser);

        assertThatThrownBy(() -> useCase.execute(new AuthenticateUserUseCase.Input("user@example.com", "StrongPass1")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void rejectsAuthenticationForASuspendedUserWithADistinctErrorFromInvalidCredentials() {
        User user = userRepository.findByEmail(Email.create("user@example.com")).orElseThrow();
        userRepository.update(user.suspend(Instant.now()));

        assertThatThrownBy(() -> useCase.execute(new AuthenticateUserUseCase.Input("user@example.com", "StrongPass1")))
                .isInstanceOf(AccountSuspendedException.class);
    }

    @Test
    void aSuspendedUserWithAnIncorrectPasswordStillGetsInvalidCredentialsNotTheSuspensionMessage() {
        // A senha é verificada antes da suspensão — ninguém descobre que uma conta está suspensa sem antes
        // provar que conhece a senha correta (ver ADR-0024).
        User user = userRepository.findByEmail(Email.create("user@example.com")).orElseThrow();
        userRepository.update(user.suspend(Instant.now()));

        assertThatThrownBy(() -> useCase.execute(new AuthenticateUserUseCase.Input("user@example.com", "WrongPass1")))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}
