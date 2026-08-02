package com.financepulse.engine.application.usecases.dashboard;

import com.financepulse.engine.application.ports.AccountRepository;
import com.financepulse.engine.application.ports.BudgetRepository;
import com.financepulse.engine.application.ports.CategoryRepository;
import com.financepulse.engine.application.ports.Clock;
import com.financepulse.engine.application.ports.IdGenerator;
import com.financepulse.engine.application.ports.PulseScoreRepository;
import com.financepulse.engine.application.ports.TransactionRepository;
import com.financepulse.engine.application.services.AccountBalanceCalculator;
import com.financepulse.engine.application.services.BudgetConsumptionCalculator;
import com.financepulse.engine.application.services.BudgetPeriodCalculator;
import com.financepulse.engine.application.services.PulseScoreCalculator;
import com.financepulse.engine.application.services.SpendingByCategoryCalculator;
import com.financepulse.engine.domain.account.Account;
import com.financepulse.engine.domain.category.Category;
import com.financepulse.engine.domain.pulsescore.PulseScoreSnapshot;
import com.financepulse.engine.domain.transaction.Transaction;
import com.financepulse.engine.domain.transaction.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * RF-033 (saldo consolidado, fluxo de caixa recente, distribuição de gastos)
 * e RF-034/RF-036 (Pulse Score atual com detalhamento por fator — ver
 * ADR-0020). Cada chamada recalcula e persiste um snapshot diário do Pulse
 * Score (RN-005, sem scheduler dedicado nesta fase).
 */
public class GetDashboardUseCase {

    static final int DEFAULT_WINDOW_DAYS = 30;
    static final int MAX_WINDOW_DAYS = 365;

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final BudgetRepository budgetRepository;
    private final PulseScoreRepository pulseScoreRepository;
    private final IdGenerator idGenerator;
    private final Clock clock;

    public GetDashboardUseCase(
            AccountRepository accountRepository,
            TransactionRepository transactionRepository,
            CategoryRepository categoryRepository,
            BudgetRepository budgetRepository,
            PulseScoreRepository pulseScoreRepository,
            IdGenerator idGenerator,
            Clock clock) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
        this.budgetRepository = budgetRepository;
        this.pulseScoreRepository = pulseScoreRepository;
        this.idGenerator = idGenerator;
        this.clock = clock;
    }

    public Output execute(Input input) {
        LocalDate today = clock.today();
        int windowDays = Math.min(Math.max(input.windowDays(), 1), MAX_WINDOW_DAYS);
        LocalDate since = today.minusDays(windowDays);

        List<Account> activeAccounts = accountRepository.findAllByUserId(input.userId()).stream()
                .filter(account -> !account.isArchived())
                .toList();
        List<Transaction> allTransactions = transactionRepository.findAllByUserId(input.userId());
        Map<String, List<Transaction>> transactionsByAccount =
                allTransactions.stream().collect(Collectors.groupingBy(Transaction::getAccountId));

        BigDecimal consolidatedBalance = sumBalances(activeAccounts, transactionsByAccount, transactions -> transactions);
        BigDecimal balancePast = sumBalances(
                activeAccounts, transactionsByAccount, transactions -> transactions.stream().filter(t -> !t.getDate().isAfter(since)).toList());

        List<Transaction> windowTransactions =
                allTransactions.stream().filter(t -> !t.getDate().isBefore(since) && !t.getDate().isAfter(today)).toList();
        BigDecimal totalIncome = sumByType(windowTransactions, TransactionType.INCOME);

        SpendingByCategoryCalculator.Result spendingResult = SpendingByCategoryCalculator.calculate(windowTransactions);
        BigDecimal totalExpense = spendingResult.totalExpense();
        Map<String, BigDecimal> expenseByCategory = spendingResult.categories().stream()
                .collect(Collectors.toMap(SpendingByCategoryCalculator.CategoryAmount::categoryId, SpendingByCategoryCalculator.CategoryAmount::amount));

        List<CategorySpending> spendingByCategory = toSpendingByCategoryView(spendingResult, input.userId());

        List<BigDecimal> budgetConsumptionPercentages = budgetConsumptionPercentages(input.userId(), allTransactions, today);

        PulseScoreCalculator.Result pulseResult = PulseScoreCalculator.calculate(new PulseScoreCalculator.Input(
                budgetConsumptionPercentages, totalIncome, totalExpense, expenseByCategory, consolidatedBalance, balancePast));

        persistSnapshot(input.userId(), today, pulseResult);

        return new Output(
                consolidatedBalance,
                new CashFlow(windowDays, totalIncome, totalExpense, totalIncome.subtract(totalExpense)),
                spendingByCategory,
                new PulseScoreView(pulseResult.overallScore(), PulseScoreCalculator.FORMULA_VERSION, pulseResult.factors()));
    }

    private BigDecimal sumBalances(
            List<Account> accounts,
            Map<String, List<Transaction>> transactionsByAccount,
            java.util.function.Function<List<Transaction>, List<Transaction>> transactionFilter) {
        return accounts.stream()
                .map(account -> AccountBalanceCalculator.currentBalance(
                        account, transactionFilter.apply(transactionsByAccount.getOrDefault(account.getId(), List.of()))))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<CategorySpending> toSpendingByCategoryView(SpendingByCategoryCalculator.Result result, String userId) {
        Map<String, String> categoryNames =
                categoryRepository.findAllByUserId(userId).stream().collect(Collectors.toMap(Category::getId, Category::getName));

        return result.categories().stream()
                .map(category -> new CategorySpending(
                        category.categoryId(), categoryNames.getOrDefault(category.categoryId(), category.categoryId()), category.amount(),
                        category.percentage()))
                .toList();
    }

    private List<BigDecimal> budgetConsumptionPercentages(String userId, List<Transaction> allTransactions, LocalDate today) {
        return budgetRepository.findAllByUserId(userId).stream()
                .map(budget -> {
                    var period = BudgetPeriodCalculator.currentPeriod(budget, today);
                    List<Transaction> categoryTransactions =
                            allTransactions.stream().filter(t -> t.getCategoryId().equals(budget.getCategoryId())).toList();
                    return BudgetConsumptionCalculator.calculate(budget, period, categoryTransactions).percentage();
                })
                .toList();
    }

    private void persistSnapshot(String userId, LocalDate today, PulseScoreCalculator.Result result) {
        Map<String, BigDecimal> scoreByFactorName =
                result.factors().stream().collect(Collectors.toMap(PulseScoreCalculator.Factor::name, PulseScoreCalculator.Factor::score));

        PulseScoreSnapshot snapshot = PulseScoreSnapshot.create(
                idGenerator.generate(),
                userId,
                today,
                result.overallScore(),
                scoreByFactorName.get("budgetConsistency"),
                scoreByFactorName.get("savingsRate"),
                scoreByFactorName.get("spendingDiversification"),
                scoreByFactorName.get("balanceTrend"),
                PulseScoreCalculator.FORMULA_VERSION);

        pulseScoreRepository.saveOrUpdate(snapshot);
    }

    private static BigDecimal sumByType(List<Transaction> transactions, TransactionType type) {
        return transactions.stream().filter(t -> t.getType() == type).map(Transaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public record Input(String userId, int windowDays) {

        public Input(String userId) {
            this(userId, DEFAULT_WINDOW_DAYS);
        }
    }

    public record Output(BigDecimal consolidatedBalance, CashFlow cashFlow, List<CategorySpending> spendingByCategory, PulseScoreView pulseScore) {
    }

    public record CashFlow(int windowDays, BigDecimal totalIncome, BigDecimal totalExpense, BigDecimal net) {
    }

    public record CategorySpending(String categoryId, String categoryName, BigDecimal amount, BigDecimal percentage) {
    }

    public record PulseScoreView(BigDecimal overallScore, String formulaVersion, List<PulseScoreCalculator.Factor> factors) {
    }
}
