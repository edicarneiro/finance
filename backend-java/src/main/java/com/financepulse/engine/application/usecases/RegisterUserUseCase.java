package com.financepulse.engine.application.usecases;

import com.financepulse.engine.application.ports.IdGenerator;
import com.financepulse.engine.application.ports.PasswordHasher;
import com.financepulse.engine.application.ports.UserRepository;
import com.financepulse.engine.domain.user.Email;
import com.financepulse.engine.domain.user.PasswordPolicy;
import com.financepulse.engine.domain.user.User;
import com.financepulse.engine.domain.user.errors.DuplicateEmailException;

public class RegisterUserUseCase {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final IdGenerator idGenerator;

    public RegisterUserUseCase(UserRepository userRepository, PasswordHasher passwordHasher, IdGenerator idGenerator) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.idGenerator = idGenerator;
    }

    public Output execute(Input input) {
        Email email = Email.create(input.email());
        PasswordPolicy.assertStrongPassword(input.password());

        if (userRepository.findByEmail(email).isPresent()) {
            throw new DuplicateEmailException(email.toString());
        }

        String passwordHash = passwordHasher.hash(input.password());
        User user = User.register(idGenerator.generate(), email, passwordHash);

        userRepository.save(user);

        return new Output(user.getId());
    }

    public record Input(String email, String password) {
    }

    public record Output(String userId) {
    }
}
