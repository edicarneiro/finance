package com.financepulse.engine.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.financepulse.engine.domain.user.Email;
import com.financepulse.engine.domain.user.User;
import com.financepulse.engine.domain.user.errors.DuplicateEmailException;
import com.financepulse.engine.domain.user.errors.WeakPasswordException;
import com.financepulse.engine.testsupport.FakePasswordHasher;
import com.financepulse.engine.testsupport.InMemoryUserRepository;
import com.financepulse.engine.testsupport.SequentialIdGenerator;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RegisterUserUseCaseTest {

    private InMemoryUserRepository userRepository;
    private RegisterUserUseCase useCase;

    @BeforeEach
    void setUp() {
        userRepository = new InMemoryUserRepository();
        useCase = new RegisterUserUseCase(userRepository, new FakePasswordHasher(), new SequentialIdGenerator("user"));
    }

    @Test
    void registersANewUserAndPersistsAHashedPassword() {
        RegisterUserUseCase.Output result = useCase.execute(new RegisterUserUseCase.Input("user@example.com", "StrongPass1"));

        assertThat(result.userId()).isEqualTo("user-1");

        Optional<User> saved = userRepository.findByEmail(Email.create("user@example.com"));
        assertThat(saved).isPresent();
        assertThat(saved.get().getPasswordHash()).isEqualTo("hashed:StrongPass1");
        assertThat(saved.get().getPasswordHash()).isNotEqualTo("StrongPass1");
    }

    @Test
    void rejectsRegistrationWithAnEmailAlreadyInUseCaseInsensitively() {
        useCase.execute(new RegisterUserUseCase.Input("user@example.com", "StrongPass1"));

        assertThatThrownBy(() -> useCase.execute(new RegisterUserUseCase.Input("USER@example.com", "AnotherPass1")))
                .isInstanceOf(DuplicateEmailException.class);
    }

    @Test
    void rejectsAPasswordShorterThanTheMinimumLength() {
        assertThatThrownBy(() -> useCase.execute(new RegisterUserUseCase.Input("user@example.com", "short")))
                .isInstanceOf(WeakPasswordException.class);
    }

    @Test
    void doesNotPersistAUserWhenThePasswordIsWeak() {
        assertThatThrownBy(() -> useCase.execute(new RegisterUserUseCase.Input("user@example.com", "short")));

        Optional<User> saved = userRepository.findByEmail(Email.create("user@example.com"));
        assertThat(saved).isEmpty();
    }
}
