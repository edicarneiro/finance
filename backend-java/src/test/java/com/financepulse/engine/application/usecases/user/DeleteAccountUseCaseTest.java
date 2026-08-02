package com.financepulse.engine.application.usecases.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.financepulse.engine.application.usecases.AuthenticateUserUseCase;
import com.financepulse.engine.application.usecases.RegisterUserUseCase;
import com.financepulse.engine.domain.user.User;
import com.financepulse.engine.domain.user.errors.InvalidCredentialsException;
import com.financepulse.engine.testsupport.FakePasswordHasher;
import com.financepulse.engine.testsupport.FakeTokenService;
import com.financepulse.engine.testsupport.InMemoryUserRepository;
import com.financepulse.engine.testsupport.SequentialIdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DeleteAccountUseCaseTest {

    private InMemoryUserRepository userRepository;
    private FakePasswordHasher passwordHasher;
    private DeleteAccountUseCase useCase;
    private String userId;

    @BeforeEach
    void setUp() {
        userRepository = new InMemoryUserRepository();
        passwordHasher = new FakePasswordHasher();
        useCase = new DeleteAccountUseCase(userRepository, passwordHasher, new SequentialIdGenerator("gen"));

        RegisterUserUseCase registerUser = new RegisterUserUseCase(userRepository, passwordHasher, new SequentialIdGenerator("user"));
        userId = registerUser.execute(new RegisterUserUseCase.Input("user@example.com", "StrongPass1")).userId();
    }

    @Test
    void anonymizesTheUserWhenThePasswordIsCorrect() {
        useCase.execute(new DeleteAccountUseCase.Input(userId, "StrongPass1"));

        User anonymized = userRepository.findById(userId).orElseThrow();
        assertThat(anonymized.isDeleted()).isTrue();
        assertThat(anonymized.getEmail().toString()).contains("@anonymized.financepulse.internal");
        assertThat(anonymized.getName()).isNull();
    }

    @Test
    void preservesTheUserIdAfterAnonymization() {
        useCase.execute(new DeleteAccountUseCase.Input(userId, "StrongPass1"));

        assertThat(userRepository.findById(userId)).isPresent();
    }

    @Test
    void rejectsAnIncorrectPassword() {
        assertThatThrownBy(() -> useCase.execute(new DeleteAccountUseCase.Input(userId, "WrongPass1")))
                .isInstanceOf(InvalidCredentialsException.class);

        assertThat(userRepository.findById(userId).orElseThrow().isDeleted()).isFalse();
    }

    @Test
    void aSecondDeletionAttemptIsNaturallyRejectedBecauseTheOriginalPasswordNeverMatchesAgain() {
        useCase.execute(new DeleteAccountUseCase.Input(userId, "StrongPass1"));

        assertThatThrownBy(() -> useCase.execute(new DeleteAccountUseCase.Input(userId, "StrongPass1")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void aDeletedUserCanNoLongerAuthenticate() {
        useCase.execute(new DeleteAccountUseCase.Input(userId, "StrongPass1"));

        AuthenticateUserUseCase authenticateUserUseCase = new AuthenticateUserUseCase(userRepository, passwordHasher, new FakeTokenService());
        assertThatThrownBy(() -> authenticateUserUseCase.execute(new AuthenticateUserUseCase.Input("user@example.com", "StrongPass1")))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}
