package com.financepulse.engine.adapters.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.financepulse.engine.domain.goal.Goal;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

/** Exercita o adaptador contra H2 real, mesma disciplina dos demais adaptadores de persistência (rules.md § 3). */
@DataJpaTest
class JpaGoalRepositoryAdapterTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 7, 31);

    @Autowired
    private SpringDataGoalJpaRepository jpaRepository;

    @Test
    void savesAndReloadsAnAccountBasedGoal() {
        JpaGoalRepositoryAdapter adapter = new JpaGoalRepositoryAdapter(jpaRepository);
        Goal goal = Goal.create(
                "goal-1", "user-1", "Reserva", new BigDecimal("10000.00"), LocalDate.of(2026, 12, 31), "account-1", null, List.of(80, 100), TODAY);

        adapter.save(goal);

        Optional<Goal> found = adapter.findByIdAndUserId("goal-1", "user-1");
        assertThat(found).isPresent();
        assertThat(found.get().getTargetAmount()).isEqualByComparingTo("10000.00");
        assertThat(found.get().getAccountId()).contains("account-1");
        assertThat(found.get().getCategoryId()).isEmpty();
        assertThat(found.get().getProgressAlertThresholds()).containsExactlyInAnyOrder(80, 100);
    }

    @Test
    void savesAndReloadsACategoryBasedGoal() {
        JpaGoalRepositoryAdapter adapter = new JpaGoalRepositoryAdapter(jpaRepository);
        Goal goal = Goal.create("goal-1", "user-1", "Viagem", new BigDecimal("5000.00"), LocalDate.of(2026, 12, 31), null, "category-1", null, TODAY);

        adapter.save(goal);

        Goal found = adapter.findByIdAndUserId("goal-1", "user-1").orElseThrow();
        assertThat(found.getAccountId()).isEmpty();
        assertThat(found.getCategoryId()).contains("category-1");
    }

    @Test
    void doesNotFindAGoalBelongingToAnotherUser() {
        JpaGoalRepositoryAdapter adapter = new JpaGoalRepositoryAdapter(jpaRepository);
        adapter.save(Goal.create("goal-1", "user-1", "Reserva", BigDecimal.TEN, LocalDate.of(2026, 12, 31), "account-1", null, null, TODAY));

        assertThat(adapter.findByIdAndUserId("goal-1", "another-user")).isEmpty();
    }

    @Test
    void listsAllGoalsForAUser() {
        JpaGoalRepositoryAdapter adapter = new JpaGoalRepositoryAdapter(jpaRepository);
        adapter.save(Goal.create("goal-1", "user-1", "Reserva", BigDecimal.TEN, LocalDate.of(2026, 12, 31), "account-1", null, null, TODAY));
        adapter.save(Goal.create("goal-2", "user-1", "Viagem", BigDecimal.TEN, LocalDate.of(2026, 12, 31), null, "category-1", null, TODAY));
        adapter.save(Goal.create("goal-3", "user-2", "Outra", BigDecimal.TEN, LocalDate.of(2026, 12, 31), "account-1", null, null, TODAY));

        List<Goal> goals = adapter.findAllByUserId("user-1");

        assertThat(goals).hasSize(2).extracting(Goal::getId).containsExactlyInAnyOrder("goal-1", "goal-2");
    }

    @Test
    void persistsAnUpdateToAnExistingGoal() {
        JpaGoalRepositoryAdapter adapter = new JpaGoalRepositoryAdapter(jpaRepository);
        Goal goal = Goal.create("goal-1", "user-1", "Reserva", new BigDecimal("1000.00"), LocalDate.of(2026, 12, 31), "account-1", null, null, TODAY);
        adapter.save(goal);

        adapter.update(goal.withDetails("Reserva de emergência", new BigDecimal("2000.00"), LocalDate.of(2027, 6, 30), List.of(90), TODAY));

        Goal reloaded = adapter.findByIdAndUserId("goal-1", "user-1").orElseThrow();
        assertThat(reloaded.getName()).isEqualTo("Reserva de emergência");
        assertThat(reloaded.getTargetAmount()).isEqualByComparingTo("2000.00");
        assertThat(reloaded.getProgressAlertThresholds()).containsExactly(90);
    }

    @Test
    void deletesAGoalScopedToItsOwner() {
        JpaGoalRepositoryAdapter adapter = new JpaGoalRepositoryAdapter(jpaRepository);
        adapter.save(Goal.create("goal-1", "user-1", "Reserva", BigDecimal.TEN, LocalDate.of(2026, 12, 31), "account-1", null, null, TODAY));

        adapter.deleteByIdAndUserId("goal-1", "another-user");
        assertThat(adapter.findByIdAndUserId("goal-1", "user-1"))
                .as("delete de outro usuário não deve remover a meta")
                .isPresent();

        adapter.deleteByIdAndUserId("goal-1", "user-1");
        assertThat(adapter.findByIdAndUserId("goal-1", "user-1")).isEmpty();
    }
}
