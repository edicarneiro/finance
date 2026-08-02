package com.financepulse.engine.adapters.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.financepulse.engine.domain.user.ConsentRecord;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

/** Exercita o adaptador contra H2 real, mesma disciplina dos demais adaptadores de persistência (rules.md § 3). */
@DataJpaTest
class JpaConsentRepositoryAdapterTest {

    @Autowired
    private SpringDataConsentJpaRepository jpaRepository;

    @Test
    void savesAndReloadsAConsentRecord() {
        JpaConsentRepositoryAdapter adapter = new JpaConsentRepositoryAdapter(jpaRepository);

        adapter.save(ConsentRecord.create("consent-1", "user-1", "2026-08-01"));

        List<ConsentRecord> found = adapter.findAllByUserId("user-1");
        assertThat(found).hasSize(1);
        assertThat(found.get(0).getVersion()).isEqualTo("2026-08-01");
    }

    @Test
    void keepsMultipleAcceptancesAsAnAppendOnlyHistory() {
        JpaConsentRepositoryAdapter adapter = new JpaConsentRepositoryAdapter(jpaRepository);

        adapter.save(ConsentRecord.create("consent-1", "user-1", "2026-08-01"));
        adapter.save(ConsentRecord.create("consent-2", "user-1", "2026-09-01"));

        assertThat(adapter.findAllByUserId("user-1")).hasSize(2);
    }

    @Test
    void doesNotMixConsentsFromAnotherUser() {
        JpaConsentRepositoryAdapter adapter = new JpaConsentRepositoryAdapter(jpaRepository);
        adapter.save(ConsentRecord.create("consent-1", "user-1", "2026-08-01"));

        assertThat(adapter.findAllByUserId("another-user")).isEmpty();
    }
}
