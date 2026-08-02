package com.financepulse.engine.adapters.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.financepulse.engine.domain.pulsescore.PulseScoreSnapshot;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

/** Exercita o adaptador contra H2 real, mesma disciplina dos demais adaptadores de persistência (rules.md § 3). */
@DataJpaTest
class JpaPulseScoreRepositoryAdapterTest {

    @Autowired
    private SpringDataPulseScoreJpaRepository jpaRepository;

    @Test
    void savesAndReloadsASnapshotWithAllFourFactors() {
        JpaPulseScoreRepositoryAdapter adapter = new JpaPulseScoreRepositoryAdapter(jpaRepository);
        PulseScoreSnapshot snapshot = PulseScoreSnapshot.create(
                "snap-1", "user-1", LocalDate.of(2026, 7, 31), new BigDecimal("68.75"), new BigDecimal("90"), new BigDecimal("80"),
                new BigDecimal("50"), new BigDecimal("55"), "pulse-v0-provisional");

        adapter.saveOrUpdate(snapshot);

        List<PulseScoreSnapshot> found = adapter.findAllByUserId("user-1");
        assertThat(found).hasSize(1);
        assertThat(found.get(0).getOverallScore()).isEqualByComparingTo("68.75");
        assertThat(found.get(0).getBudgetConsistencyScore()).contains(new BigDecimal("90"));
        assertThat(found.get(0).getBalanceTrendScore()).isEqualByComparingTo("55");
        assertThat(found.get(0).getFormulaVersion()).isEqualTo("pulse-v0-provisional");
    }

    @Test
    void toleratesOmittedFactorsBeingPersistedAsNull() {
        JpaPulseScoreRepositoryAdapter adapter = new JpaPulseScoreRepositoryAdapter(jpaRepository);
        PulseScoreSnapshot snapshot = PulseScoreSnapshot.create(
                "snap-1", "user-1", LocalDate.of(2026, 7, 31), new BigDecimal("50"), null, null, null, new BigDecimal("50"),
                "pulse-v0-provisional");

        adapter.saveOrUpdate(snapshot);

        PulseScoreSnapshot found = adapter.findAllByUserId("user-1").get(0);
        assertThat(found.getBudgetConsistencyScore()).isEmpty();
        assertThat(found.getSavingsRateScore()).isEmpty();
        assertThat(found.getSpendingDiversificationScore()).isEmpty();
    }

    @Test
    void upsertsInPlaceInsteadOfCreatingASecondSnapshotForTheSameUserAndDate() {
        JpaPulseScoreRepositoryAdapter adapter = new JpaPulseScoreRepositoryAdapter(jpaRepository);
        LocalDate today = LocalDate.of(2026, 7, 31);
        adapter.saveOrUpdate(PulseScoreSnapshot.create(
                "snap-1", "user-1", today, new BigDecimal("40"), null, null, null, new BigDecimal("40"), "pulse-v0-provisional"));

        adapter.saveOrUpdate(PulseScoreSnapshot.create(
                "snap-2", "user-1", today, new BigDecimal("60"), null, null, null, new BigDecimal("60"), "pulse-v0-provisional"));

        List<PulseScoreSnapshot> found = adapter.findAllByUserId("user-1");
        assertThat(found).hasSize(1);
        assertThat(found.get(0).getOverallScore()).isEqualByComparingTo("60");
    }

    @Test
    void doesNotFindSnapshotsBelongingToAnotherUser() {
        JpaPulseScoreRepositoryAdapter adapter = new JpaPulseScoreRepositoryAdapter(jpaRepository);
        adapter.saveOrUpdate(PulseScoreSnapshot.create(
                "snap-1", "user-1", LocalDate.of(2026, 7, 31), new BigDecimal("50"), null, null, null, new BigDecimal("50"),
                "pulse-v0-provisional"));

        assertThat(adapter.findAllByUserId("another-user")).isEmpty();
    }
}
