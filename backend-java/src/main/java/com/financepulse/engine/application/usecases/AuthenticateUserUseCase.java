package com.financepulse.engine.application.usecases;

import com.financepulse.engine.application.ports.PasswordHasher;
import com.financepulse.engine.application.ports.TokenService;
import com.financepulse.engine.application.ports.UserRepository;
import com.financepulse.engine.domain.user.Email;
import com.financepulse.engine.domain.user.User;
import com.financepulse.engine.domain.user.errors.AccountSuspendedException;
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

        // Mesma mensagem de erro para "e-mail não encontrado", "senha incorreta" e "conta excluída"
        // (padrão anti-enumeração, replicado do backend TypeScript). A checagem de isDeleted() é defesa em
        // profundidade — o hash anonimizado (ADR-0023) já torna a senha original incapaz de conferir.
        if (user.isEmpty() || user.get().isDeleted() || !passwordHasher.matches(input.password(), user.get().getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        // Só verificada depois de confirmar a senha — evita que alguém sem a senha correta descubra que uma
        // conta está suspensa. Diferente de conta excluída, aqui um retorno acionável é intencional (ver ADR-0024).
        if (user.get().isSuspended()) {
            throw new AccountSuspendedException();
        }

        String token = tokenService.issue(user.get().getId());

        return new Output(token);
    }

    public record Input(String email, String password) {
    }

    public record Output(String token) {
    }
}
