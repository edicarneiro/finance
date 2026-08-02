package com.financepulse.engine.adapters.in.web.dto;

import com.financepulse.engine.application.usecases.user.ExportUserDataUseCase.AccountExport;
import com.financepulse.engine.application.usecases.user.ExportUserDataUseCase.BudgetExport;
import com.financepulse.engine.application.usecases.user.ExportUserDataUseCase.CategoryExport;
import com.financepulse.engine.application.usecases.user.ExportUserDataUseCase.ConsentExport;
import com.financepulse.engine.application.usecases.user.ExportUserDataUseCase.GoalExport;
import com.financepulse.engine.application.usecases.user.ExportUserDataUseCase.NotificationExport;
import com.financepulse.engine.application.usecases.user.ExportUserDataUseCase.NotificationPreferenceExport;
import com.financepulse.engine.application.usecases.user.ExportUserDataUseCase.Output;
import com.financepulse.engine.application.usecases.user.ExportUserDataUseCase.ProfileExport;
import com.financepulse.engine.application.usecases.user.ExportUserDataUseCase.PulseScoreExport;
import com.financepulse.engine.application.usecases.user.ExportUserDataUseCase.TransactionExport;
import com.financepulse.engine.domain.account.AccountType;
import com.financepulse.engine.domain.budget.BudgetPeriodType;
import com.financepulse.engine.domain.notification.AlertType;
import com.financepulse.engine.domain.notification.NotificationChannel;
import com.financepulse.engine.domain.transaction.TransactionType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/** RF-044 (ver ADR-0023): documento único com todos os dados pessoais e financeiros do usuário — nunca inclui senha/hash. */
public record UserDataExportResponse(
        ProfileResponse profile,
        List<AccountItem> accounts,
        List<TransactionItem> transactions,
        List<CategoryItem> categories,
        List<BudgetItem> budgets,
        List<GoalItem> goals,
        List<PulseScoreItem> pulseScoreHistory,
        List<NotificationItem> notifications,
        List<NotificationPreferenceItem> notificationPreferences,
        List<ConsentItem> consentHistory) {

    public static UserDataExportResponse from(Output output) {
        return new UserDataExportResponse(
                ProfileResponse.from(output.profile()),
                output.accounts().stream().map(AccountItem::from).toList(),
                output.transactions().stream().map(TransactionItem::from).toList(),
                output.categories().stream().map(CategoryItem::from).toList(),
                output.budgets().stream().map(BudgetItem::from).toList(),
                output.goals().stream().map(GoalItem::from).toList(),
                output.pulseScoreHistory().stream().map(PulseScoreItem::from).toList(),
                output.notifications().stream().map(NotificationItem::from).toList(),
                output.notificationPreferences().stream().map(NotificationPreferenceItem::from).toList(),
                output.consentHistory().stream().map(ConsentItem::from).toList());
    }

    public record ProfileResponse(String id, String email, String name, Instant createdAt, Instant deletedAt) {
        public static ProfileResponse from(ProfileExport export) {
            return new ProfileResponse(export.id(), export.email(), export.name(), export.createdAt(), export.deletedAt());
        }
    }

    public record AccountItem(String id, AccountType type, String name, String currency, BigDecimal balance, boolean archived, Instant createdAt) {
        public static AccountItem from(AccountExport export) {
            return new AccountItem(export.id(), export.type(), export.name(), export.currency(), export.balance(), export.archived(),
                    export.createdAt());
        }
    }

    public record TransactionItem(
            String id, String accountId, String categoryId, TransactionType type, BigDecimal amount, LocalDate date, String description,
            List<String> tags, Instant createdAt) {
        public static TransactionItem from(TransactionExport export) {
            return new TransactionItem(export.id(), export.accountId(), export.categoryId(), export.type(), export.amount(), export.date(),
                    export.description(), export.tags(), export.createdAt());
        }
    }

    public record CategoryItem(String id, String name, String parentCategoryId, Instant createdAt) {
        public static CategoryItem from(CategoryExport export) {
            return new CategoryItem(export.id(), export.name(), export.parentCategoryId(), export.createdAt());
        }
    }

    public record BudgetItem(
            String id, String categoryId, BigDecimal limitAmount, BudgetPeriodType periodType, LocalDate customPeriodStart,
            LocalDate customPeriodEnd, List<Integer> alertThresholds, Instant createdAt) {
        public static BudgetItem from(BudgetExport export) {
            return new BudgetItem(export.id(), export.categoryId(), export.limitAmount(), export.periodType(), export.customPeriodStart(),
                    export.customPeriodEnd(), export.alertThresholds(), export.createdAt());
        }
    }

    public record GoalItem(
            String id, String name, BigDecimal targetAmount, LocalDate deadline, String accountId, String categoryId,
            List<Integer> progressAlertThresholds, Instant createdAt) {
        public static GoalItem from(GoalExport export) {
            return new GoalItem(export.id(), export.name(), export.targetAmount(), export.deadline(), export.accountId(), export.categoryId(),
                    export.progressAlertThresholds(), export.createdAt());
        }
    }

    public record PulseScoreItem(
            LocalDate scoreDate, BigDecimal overallScore, String formulaVersion, BigDecimal budgetConsistencyScore, BigDecimal savingsRateScore,
            BigDecimal spendingDiversificationScore, BigDecimal balanceTrendScore) {
        public static PulseScoreItem from(PulseScoreExport export) {
            return new PulseScoreItem(export.scoreDate(), export.overallScore(), export.formulaVersion(), export.budgetConsistencyScore(),
                    export.savingsRateScore(), export.spendingDiversificationScore(), export.balanceTrendScore());
        }
    }

    public record NotificationItem(
            String id, AlertType alertType, String message, Set<NotificationChannel> deliveredChannels, boolean read, Instant createdAt) {
        public static NotificationItem from(NotificationExport export) {
            return new NotificationItem(export.id(), export.alertType(), export.message(), export.deliveredChannels(), export.read(),
                    export.createdAt());
        }
    }

    public record NotificationPreferenceItem(AlertType alertType, NotificationChannel channel, boolean enabled) {
        public static NotificationPreferenceItem from(NotificationPreferenceExport export) {
            return new NotificationPreferenceItem(export.alertType(), export.channel(), export.enabled());
        }
    }

    public record ConsentItem(String version, Instant acceptedAt) {
        public static ConsentItem from(ConsentExport export) {
            return new ConsentItem(export.version(), export.acceptedAt());
        }
    }
}
