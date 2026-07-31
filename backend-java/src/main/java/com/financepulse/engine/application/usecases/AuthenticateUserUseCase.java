package com.financepulse.engine.application.usecases;

import com.financepulse.engine.application.ports.PasswordHasher;
import com.financepulse.engine.application.ports.TokenService;
import com.financepulse.engine.application.ports.UserRepository;
import com.financepulse.engine.domain.user.Email;
import com.financepulse.engine.domain.user.User;
import com.financepulse.engine.domain.user.errors.InvalidCredentialsException;
import java.util.Optional;

public class AuthenticateUserUseCase {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final TokenService tokenService;

    public AuthenticateUserUseCase(UserRepository userRepository, PasswordHasher passwordHasher, TokenService tokenService) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.tokenService = tokenService;
    }

    public Output execute(Input input) {
        Email email = Email.create(input.email());

        Optional<User> user = userRepository.findByEmail(email);

        // Mesma mensagem de erro para "e-mail não encontrado" e "senha incorreta"
        // (padrão anti-enumeração, replicado do backend TypeScript).
        if (user.isEmpty() || !passwordHasher.matches(input.password(), user.get().getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        String token = tokenService.issue(user.get().getId());

        return new Output(token);
    }

    public record Input(String email, String password) {
    }

    public record Output(String token) {
    }
}
