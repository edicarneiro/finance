package com.financepulse.engine.adapters.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.financepulse.engine.domain.budget.Budget;
import com.financepulse.engine.domain.budget.BudgetPeriodType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

/** Exercita o adaptador contra H2 real, mesma disciplina dos demais adaptadores de persistência (rules.md § 3). */
@DataJpaTest
class JpaBudgetRepositoryAdapterTest {

    @Autowired
    private SpringDataBudgetJpaRepository jpaRepository;

    @Test
    void savesAndReloadsAMonthlyBudgetWithThresholds() {
        JpaBudgetRepositoryAdapter adapter = new JpaBudgetRepositoryAdapter(jpaRepository);
        Budget budget = Budget.create(
                "budget-1", "user-1", "category-1", new BigDecimal("500.00"), BudgetPeriodType.MONTHLY, null, null, List.of(80, 100));

        adapter.save(budget);

        Optional<Budget> found = adapter.findByIdAndUserId("budget-1", "user-1");
        assertThat(found).isPresent();
        assertThat(found.get().getLimitAmount()).isEqualByComparingTo("500.00");
        assertThat(found.get().getAlertThresholds()).containsExactlyInAnyOrder(80, 100);
        assertThat(found.get().getCustomPeriodStart()).isEmpty();
    }

    @Test
    void savesAndReloadsACustomBudgetWithItsDateRange() {
        JpaBudgetRepositoryAdapter adapter = new JpaBudgetRepositoryAdapter(jpaRepository);
        Budget budget = Budget.create(
                "budget-1",
                "user-1",
                "category-1",
                new BigDecimal("200.00"),
                BudgetPeriodType.CUSTOM,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 31),
                null);

        adapter.save(budget);

        Budget found = adapter.findByIdAndUserId("budget-1", "user-1").orElseThrow();
        assertThat(found.getCustomPeriodStart()).contains(LocalDate.of(2026, 1, 1));
        assertThat(found.getCustomPeriodEnd()).contains(LocalDate.of(2026, 1, 31));
    }

    @Test
    void doesNotFindABudgetBelongingToAnotherUser() {
        JpaBudgetRepositoryAdapter adapter = new JpaBudgetRepositoryAdapter(jpaRepository);
        adapter.save(Budget.create("budget-1", "user-1", "category-1", BigDecimal.TEN, BudgetPeriodType.MONTHLY, null, null, null));

        assertThat(adapter.findByIdAndUserId("budget-1", "another-user")).isEmpty();
    }

    @Test
    void listsAllBudgetsForAUser() {
        JpaBudgetRepositoryAdapter adapter = new JpaBudgetRepositoryAdapter(jpaRepository);
        adapter.save(Budget.create("budget-1", "user-1", "category-1", BigDecimal.TEN, BudgetPeriodType.MONTHLY, null, null, null));
        adapter.save(Budget.create("budget-2", "user-1", "category-2", BigDecimal.TEN, BudgetPeriodType.WEEKLY, null, null, null));
        adapter.save(Budget.create("budget-3", "user-2", "category-1", BigDecimal.TEN, BudgetPeriodType.MONTHLY, null, null, null));

        List<Budget> budgets = adapter.findAllByUserId("user-1");

        assertThat(budgets).hasSize(2).extracting(Budget::getId).containsExactlyInAnyOrder("budget-1", "budget-2");
    }

    @Test
    void persistsAnUpdateToAnExistingBudget() {
        JpaBudgetRepositoryAdapter adapter = new JpaBudgetRepositoryAdapter(jpaRepository);
        Budget budget = Budget.create("budget-1", "user-1", "category-1", new BigDecimal("100.00"), BudgetPeriodType.MONTHLY, null, null, null);
        adapter.save(budget);

        adapter.update(budget.withLimitAndThresholds(new BigDecimal("300.00"), List.of(50)));

        Budget reloaded = adapter.findByIdAndUserId("budget-1", "user-1").orElseThrow();
        assertThat(reloaded.getLimitAmount()).isEqualByComparingTo("300.00");
        assertThat(reloaded.getAlertThresholds()).containsExactly(50);
    }

    @Test
    void deletesABudgetScopedToItsOwner() {
        JpaBudgetRepositoryAdapter adapter = new JpaBudgetRepositoryAdapter(jpaRepository);
        adapter.save(Budget.create("budget-1", "user-1", "category-1", BigDecimal.TEN, BudgetPeriodType.MONTHLY, null, null, null));

        adapter.deleteByIdAndUserId("budget-1", "another-user");
        assertThat(adapter.findByIdAndUserId("budget-1", "user-1"))
                .as("delete de outro usuário não deve remover o orçamento")
                .isPresent();

        adapter.deleteByIdAndUserId("budget-1", "user-1");
        assertThat(adapter.findByIdAndUserId("budget-1", "user-1")).isEmpty();
    }
}
