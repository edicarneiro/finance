package com.financepulse.engine.adapters.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.financepulse.engine.domain.backoffice.AuditAction;
import com.financepulse.engine.domain.backoffice.AuditLogEntry;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

/** Exercita o adaptador contra H2 real, mesma disciplina dos demais adaptadores de persistência (rules.md § 3). */
@DataJpaTest
class JpaAuditLogRepositoryAdapterTest {

    @Autowired
    private SpringDataAuditLogJpaRepository jpaRepository;

    @Test
    void savesAndReloadsAnAuditLogEntry() {
        JpaAuditLogRepositoryAdapter adapter = new JpaAuditLogRepositoryAdapter(jpaRepository);

        adapter.save(AuditLogEntry.create("audit-1", "operator-1", "target-1", AuditAction.SUSPENDED_ACCOUNT, "Suspeita de fraude"));

        List<AuditLogEntry> found = adapter.findAllByTargetUserId("target-1");
        assertThat(found).hasSize(1);
        assertThat(found.get(0).getOperatorUserId()).isEqualTo("operator-1");
        assertThat(found.get(0).getAction()).isEqualTo(AuditAction.SUSPENDED_ACCOUNT);
        assertThat(found.get(0).getDetails()).isEqualTo("Suspeita de fraude");
    }

    @Test
    void keepsMultipleEntriesAsAnAppendOnlyHistory() {
        JpaAuditLogRepositoryAdapter adapter = new JpaAuditLogRepositoryAdapter(jpaRepository);

        adapter.save(AuditLogEntry.create("audit-1", "operator-1", "target-1", AuditAction.SUSPENDED_ACCOUNT, "motivo"));
        adapter.save(AuditLogEntry.create("audit-2", "operator-1", "target-1", AuditAction.REACTIVATED_ACCOUNT, null));

        assertThat(adapter.findAllByTargetUserId("target-1")).hasSize(2);
    }

    @Test
    void doesNotMixEntriesFromAnotherTargetUser() {
        JpaAuditLogRepositoryAdapter adapter = new JpaAuditLogRepositoryAdapter(jpaRepository);
        adapter.save(AuditLogEntry.create("audit-1", "operator-1", "target-1", AuditAction.VIEWED_USER_DATA, null));

        assertThat(adapter.findAllByTargetUserId("another-target")).isEmpty();
    }
}
