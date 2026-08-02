package com.financepulse.engine.composition;

import static org.assertj.core.api.Assertions.assertThat;

import com.financepulse.engine.application.ports.CategoryRepository;
import com.financepulse.engine.application.ports.PasswordHasher;
import com.financepulse.engine.application.ports.UserRepository;
import com.financepulse.engine.application.usecases.category.ListCategoriesUseCase;
import com.financepulse.engine.domain.user.Email;
import com.financepulse.engine.domain.user.User;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Ativa o perfil "dev" explicitamente só nesta classe (via @ActiveProfiles),
 * com um H2 em memória isolado (não o PostgreSQL real de application-dev.yml,
 * ver ADR-0026) — nenhum outro teste do projeto ativa esse perfil (rules.md
 * §3: composition root real, não dublês). Sobrescreve driver/usuário/senha
 * além da URL: application-dev.yml aponta para o driver do PostgreSQL, que
 * não conecta a uma URL jdbc:h2:* — os quatro precisam ser trocados juntos.
 */
@SpringBootTest
@ActiveProfiles("dev")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:dev-data-seeder-test;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "financepulse.seed.enabled=true"
})
class DevDataSeederTest {

    @Autowired
    private DevDataSeeder devDataSeeder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private PasswordHasher passwordHasher;

    @Test
    void seedsTheDevUserWithItsDefaultCategoriesOnStartup() {
        User seededUser = userRepository.findByEmail(Email.create(DevDataSeeder.SEED_USER_EMAIL)).orElseThrow();

        assertThat(passwordHasher.matches(DevDataSeeder.SEED_USER_PASSWORD, seededUser.getPasswordHash())).isTrue();

        List<String> categoryNames =
                categoryRepository.findAllByUserId(seededUser.getId()).stream().map(c -> c.getName()).toList();
        assertThat(categoryNames).containsExactlyInAnyOrderElementsOf(ListCategoriesUseCase.DEFAULT_CATEGORY_NAMES);
    }

    @Test
    void runningTheSeederAgainIsIdempotent() {
        User firstRunUser = userRepository.findByEmail(Email.create(DevDataSeeder.SEED_USER_EMAIL)).orElseThrow();

        devDataSeeder.run();

        List<User> matchingUsers = userRepository.findByEmail(Email.create(DevDataSeeder.SEED_USER_EMAIL)).stream().toList();
        assertThat(matchingUsers).hasSize(1);
        assertThat(matchingUsers.get(0).getId()).isEqualTo(firstRunUser.getId());
        assertThat(categoryRepository.findAllByUserId(firstRunUser.getId()))
                .hasSize(ListCategoriesUseCase.DEFAULT_CATEGORY_NAMES.size());
    }
}
