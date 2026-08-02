package com.financepulse.engine.composition;

import com.financepulse.engine.application.ports.UserRepository;
import com.financepulse.engine.application.usecases.RegisterUserUseCase;
import com.financepulse.engine.application.usecases.category.ListCategoriesUseCase;
import com.financepulse.engine.domain.user.Email;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Popula um usuário de teste e suas categorias padrão ao subir em modo dev
 * (vision.md Seção 14 — "Infraestrutura de desenvolvimento local e testes").
 * Reaproveita {@link RegisterUserUseCase} e {@link ListCategoriesUseCase}
 * reais — não duplica hashing de senha nem a lista de categorias padrão
 * (RF-025, já definida em {@code ListCategoriesUseCase.DEFAULT_CATEGORY_NAMES}).
 * Idempotente: seguro rodar em toda subida, não recria o usuário se já existir.
 * Só ativa com o perfil "dev" — nenhum teste deste projeto ativa esse perfil
 * (@SpringBootTest usa src/test/resources/application.properties, que não
 * define spring.profiles.active), então nunca roda durante a suíte de testes.
 */
@Component
@Profile("dev")
@ConditionalOnProperty(name = "financepulse.seed.enabled", havingValue = "true")
public class DevDataSeeder implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DevDataSeeder.class);

    public static final String SEED_USER_EMAIL = "dev@financepulse.local";
    public static final String SEED_USER_PASSWORD = "DevPassword1";

    private final UserRepository userRepository;
    private final RegisterUserUseCase registerUserUseCase;
    private final ListCategoriesUseCase listCategoriesUseCase;

    public DevDataSeeder(
            UserRepository userRepository, RegisterUserUseCase registerUserUseCase, ListCategoriesUseCase listCategoriesUseCase) {
        this.userRepository = userRepository;
        this.registerUserUseCase = registerUserUseCase;
        this.listCategoriesUseCase = listCategoriesUseCase;
    }

    @Override
    public void run(String... args) {
        String userId = userRepository
                .findByEmail(Email.create(SEED_USER_EMAIL))
                .map(user -> user.getId())
                .orElseGet(this::registerSeedUser);

        // Reaproveita o seed preguiçoso já existente (ver ListCategoriesUseCase) — não reimplementa a
        // lista de categorias padrão aqui.
        listCategoriesUseCase.execute(new ListCategoriesUseCase.Input(userId));

        logger.info("Usuário de desenvolvimento pronto: {} / {} (userId={})", SEED_USER_EMAIL, SEED_USER_PASSWORD, userId);
    }

    private String registerSeedUser() {
        RegisterUserUseCase.Output output =
                registerUserUseCase.execute(new RegisterUserUseCase.Input(SEED_USER_EMAIL, SEED_USER_PASSWORD));
        return output.userId();
    }
}
