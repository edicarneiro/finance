package com.financepulse.engine.application.usecases.dashboard;

import static org.assertj.core.api.Assertions.assertThat;

import com.financepulse.engine.application.services.PulseScoreCalculator;
import com.financepulse.engine.domain.account.Account;
import com.financepulse.engine.domain.account.AccountType;
import com.financepulse.engine.domain.account.Currency;
import com.financepulse.engine.domain.category.Category;
import com.financepulse.engine.domain.pulsescore.PulseScoreSnapshot;
import com.financepulse.engine.domain.transaction.Transaction;
import com.financepulse.engine.domain.transaction.TransactionType;
import com.financepulse.engine.testsupport.FixedClock;
import com.financepulse.engine.testsupport.InMemoryAccountRepository;
import com.financepulse.engine.testsupport.InMemoryBudgetRepository;
import com.financepulse.engine.testsupport.InMemoryCategoryRepository;
import com.financepulse.engine.testsupport.InMemoryPulseScoreRepository;
import com.financepulse.engine.testsupport.InMemoryTransactionRepository;
import com.financepulse.engine.testsupport.SequentialIdGenerator;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GetDashboardUseCaseTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 7, 31);

    private InMemoryAccountRepository accountRepository;
    private InMemoryTransactionRepository transactionRepository;
    private InMemoryCategoryRepository categoryRepository;
    private InMemoryBudgetRepository budgetRepository;
    private InMemoryPulseScoreRepository pulseScoreRepository;
    private GetDashboardUseCase useCase;
    private String foodCategoryId;

    @BeforeEach
    void setUp() {
        accountRepository = new InMemoryAccountRepository();
        transactionRepository = new InMemoryTransactionRepository();
        categoryRepository = new InMemoryCategoryRepository();
        budgetRepository = new InMemoryBudgetRepository();
        pulseScoreRepository = new InMemoryPulseScoreRepository();
        useCase = new GetDashboardUseCase(
                accountRepository,
                transactionRepository,
                categoryRepository,
                budgetRepository,
                pulseScoreRepository,
                new SequentialIdGenerator("snapshot"),
                new FixedClock(TODAY));

        Account account = Account.create("account-1", "user-1", AccountType.CHECKING, "Conta", Currency.create("BRL"), new BigDecimal("1000"));
        accountRepository.save(account);

        Category food = Category.create("category-food", "user-1", "Alimentação", null);
        categoryRepository.save(food);
        foodCategoryId = food.getId();

        transactionRepository.save(Transaction.create(
                "t-income", "user-1", "account-1", "category-salary", TransactionType.INCOME, new BigDecimal("500"), LocalDate.of(2026, 7, 15),
                "Salário", List.of()));
        transactionRepository.save(Transaction.create(
                "t-food", "user-1", "account-1", foodCategoryId, TransactionType.EXPENSE, new BigDecimal("200"), LocalDate.of(2026, 7, 20),
                "Mercado", List.of()));
        transactionRepository.save(Transaction.create(
                "t-old", "user-1", "account-1", foodCategoryId, TransactionType.EXPENSE, new BigDecimal("200"), LocalDate.of(2026, 6, 1),
                "Antiga, fora da janela", List.of()));
    }

    @Test
    void aggregatesConsolidatedBalanceCashFlowAndSpendingByCategoryWithinTheWindow() {
        GetDashboardUseCase.Output output = useCase.execute(new GetDashboardUseCase.Input("user-1", 30));

        assertThat(output.consolidatedBalance()).isEqualByComparingTo("1100");
        assertThat(output.cashFlow().windowDays()).isEqualTo(30);
        assertThat(output.cashFlow().totalIncome()).isEqualByComparingTo("500");
        assertThat(output.cashFlow().totalExpense()).isEqualByComparingTo("200");
        assertThat(output.cashFlow().net()).isEqualByComparingTo("300");
        assertThat(output.spendingByCategory()).hasSize(1);
        assertThat(output.spendingByCategory().get(0).categoryId()).isEqualTo(foodCategoryId);
        assertThat(output.spendingByCategory().get(0).categoryName()).isEqualTo("Alimentação");
        assertThat(output.spendingByCategory().get(0).amount()).isEqualByComparingTo("200");
        assertThat(output.spendingByCategory().get(0).percentage()).isEqualByComparingTo("100");
    }

    @Test
    void computesThePulseScoreFromTheSameAggregatedData() {
        GetDashboardUseCase.Output output = useCase.execute(new GetDashboardUseCase.Input("user-1", 30));

        assertThat(output.pulseScore().formulaVersion()).isEqualTo(PulseScoreCalculator.FORMULA_VERSION);
        assertThat(output.pulseScore().overallScore()).isEqualByComparingTo("56.25");
        assertThat(output.pulseScore().factors()).extracting("name").containsExactlyInAnyOrder("savingsRate", "spendingDiversification", "balanceTrend");
    }

    @Test
    void excludesArchivedAccountsFromTheConsolidatedBalance() {
        Account archived = Account.create("account-2", "user-1", AccountType.SAVINGS, "Arquivada", Currency.create("BRL"), new BigDecimal("999999")).archive();
        accountRepository.save(archived);

        GetDashboardUseCase.Output output = useCase.execute(new GetDashboardUseCase.Input("user-1", 30));

        assertThat(output.consolidatedBalance()).isEqualByComparingTo("1100");
    }

    @Test
    void persistsExactlyOneSnapshotPerUserPerDayEvenAcrossMultipleCalls() {
        useCase.execute(new GetDashboardUseCase.Input("user-1", 30));
        useCase.execute(new GetDashboardUseCase.Input("user-1", 30));

        List<PulseScoreSnapshot> snapshots = pulseScoreRepository.findAllByUserId("user-1");
        assertThat(snapshots).hasSize(1);
        assertThat(snapshots.get(0).getScoreDate()).isEqualTo(TODAY);
        assertThat(snapshots.get(0).getOverallScore()).isEqualByComparingTo("56.25");
        assertThat(snapshots.get(0).getFormulaVersion()).isEqualTo(PulseScoreCalculator.FORMULA_VERSION);
    }
}
