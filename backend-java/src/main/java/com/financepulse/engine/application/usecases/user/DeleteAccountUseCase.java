package com.financepulse.engine.application.usecases.user;

import com.financepulse.engine.application.ports.IdGenerator;
import com.financepulse.engine.application.ports.PasswordHasher;
import com.financepulse.engine.application.ports.UserRepository;
import com.financepulse.engine.domain.user.Email;
import com.financepulse.engine.domain.user.User;
import com.financepulse.engine.domain.user.errors.InvalidCredentialsException;
import java.time.Instant;

/**
 * RF-045/RF-007 (ver ADR-0023): anonimização, não exclusão física.
 * Reautenticação por senha é a confirmação explícita exigida — reaproveita
 * {@link InvalidCredentialsException} tanto para senha incorreta quanto
 * para uma conta já excluída (o hash anonimizado nunca confere), mesmo
 * padrão anti-enumeração já usado em `/auth/login`.
 */
public class DeleteAccountUseCase {

    private static final String ANONYMIZED_EMAIL_DOMAIN = "@anonymized.financepulse.internal";

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final IdGenerator idGenerator;

    public DeleteAccountUseCase(UserRepository userRepository, PasswordHasher passwordHasher, IdGenerator idGenerator) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.idGenerator = idGenerator;
    }

    public void execute(Input input) {
        User user = userRepository.findById(input.userId()).orElseThrow(InvalidCredentialsException::new);

        if (!passwordHasher.matches(input.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        Email anonymizedEmail = Email.create("deleted-" + idGenerator.generate() + ANONYMIZED_EMAIL_DOMAIN);
        String unusablePasswordHash = passwordHasher.hash(idGenerator.generate());

        userRepository.update(user.anonymize(anonymizedEmail, unusablePasswordHash, Instant.now()));
    }

    public record Input(String userId, String password) {
    }
}
