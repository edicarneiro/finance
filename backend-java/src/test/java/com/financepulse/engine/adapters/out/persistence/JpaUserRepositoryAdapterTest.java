package com.financepulse.engine.adapters.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.financepulse.engine.domain.user.Email;
import com.financepulse.engine.domain.user.Role;
import com.financepulse.engine.domain.user.User;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

/**
 * Exercita o adaptador contra H2 real (não um dublê), na mesma linha do
 * container.integration.test.ts do backend TypeScript (rules.md §3) — um
 * teste isolado à camada de persistência real evita que um bug de mapeamento
 * JPA/SQL fique escondido atrás de cobertura só com dublês em memória.
 */
@DataJpaTest
class JpaUserRepositoryAdapterTest {

    @Autowired
    private SpringDataUserJpaRepository jpaRepository;

    @Test
    void savesAndReloadsAUserByEmail() {
        JpaUserRepositoryAdapter adapter = new JpaUserRepositoryAdapter(jpaRepository);
        Email email = Email.create("user@example.com");
        User user = User.register("user-1", email, "hashed-value");

        adapter.save(user);

        Optional<User> found = adapter.findByEmail(email);
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo("user-1");
        assertThat(found.get().getPasswordHash()).isEqualTo("hashed-value");
    }

    @Test
    void returnsEmptyWhenNoUserMatchesTheEmail() {
        JpaUserRepositoryAdapter adapter = new JpaUserRepositoryAdapter(jpaRepository);

        assertThat(adapter.findByEmail(Email.create("ghost@example.com"))).isEmpty();
    }

    @Test
    void findsAUserById() {
        JpaUserRepositoryAdapter adapter = new JpaUserRepositoryAdapter(jpaRepository);
        User user = User.register("user-1", Email.create("user@example.com"), "hashed-value");
        adapter.save(user);

        assertThat(adapter.findById("user-1")).isPresent();
        assertThat(adapter.findById("missing-id")).isEmpty();
    }

    @Test
    void persistsAnonymizationAndReloadsDeletedAt() {
        JpaUserRepositoryAdapter adapter = new JpaUserRepositoryAdapter(jpaRepository);
        User user = User.register("user-1", Email.create("user@example.com"), "hashed-value");
        adapter.save(user);
        Instant deletedAt = Instant.now();

        adapter.update(user.anonymize(Email.create("deleted-user-1@anonymized.financepulse.internal"), "unusable-hash", deletedAt));

        User reloaded = adapter.findById("user-1").orElseThrow();
        assertThat(reloaded.isDeleted()).isTrue();
        assertThat(reloaded.getEmail().toString()).isEqualTo("deleted-user-1@anonymized.financepulse.internal");
        assertThat(reloaded.getName()).isNull();
    }

    @Test
    void defaultsToCustomerRoleAndPersistsPromotionToSupportOperator() {
        JpaUserRepositoryAdapter adapter = new JpaUserRepositoryAdapter(jpaRepository);
        User user = User.register("user-1", Email.create("user@example.com"), "hashed-value");
        adapter.save(user);

        assertThat(adapter.findById("user-1").orElseThrow().getRole()).isEqualTo(Role.CUSTOMER);

        User reloaded = adapter.findById("user-1").orElseThrow();
        adapter.update(User.reconstitute(
                reloaded.getId(), reloaded.getEmail(), reloaded.getPasswordHash(), reloaded.getName(), reloaded.getCreatedAt(), null,
                Role.SUPPORT_OPERATOR, null));

        assertThat(adapter.findById("user-1").orElseThrow().getRole()).isEqualTo(Role.SUPPORT_OPERATOR);
    }

    @Test
    void persistsSuspensionAndReactivation() {
        JpaUserRepositoryAdapter adapter = new JpaUserRepositoryAdapter(jpaRepository);
        User user = User.register("user-1", Email.create("user@example.com"), "hashed-value");
        adapter.save(user);
        Instant suspendedAt = Instant.now();

        adapter.update(user.suspend(suspendedAt));
        assertThat(adapter.findById("user-1").orElseThrow().isSuspended()).isTrue();

        adapter.update(adapter.findById("user-1").orElseThrow().reactivate());
        assertThat(adapter.findById("user-1").orElseThrow().isSuspended()).isFalse();
    }
}
