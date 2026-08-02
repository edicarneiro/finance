package com.financepulse.engine.application.usecases.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.financepulse.engine.application.usecases.notification.GetNotificationPreferencesUseCase.PreferenceView;
import com.financepulse.engine.domain.notification.AlertType;
import com.financepulse.engine.domain.notification.NotificationChannel;
import com.financepulse.engine.domain.notification.NotificationPreference;
import com.financepulse.engine.testsupport.InMemoryNotificationPreferenceRepository;
import java.util.List;
import org.junit.jupiter.api.Test;

class GetNotificationPreferencesUseCaseTest {

    private final InMemoryNotificationPreferenceRepository repository = new InMemoryNotificationPreferenceRepository();
    private final GetNotificationPreferencesUseCase useCase = new GetNotificationPreferencesUseCase(repository);

    @Test
    void returnsAllSixCombinationsDefaultingToEnabledWhenNothingIsConfigured() {
        GetNotificationPreferencesUseCase.Output output = useCase.execute(new GetNotificationPreferencesUseCase.Input("user-1"));

        assertThat(output.preferences()).hasSize(AlertType.values().length * NotificationChannel.values().length);
        assertThat(output.preferences()).allMatch(PreferenceView::enabled);
    }

    @Test
    void reflectsAnExplicitlyConfiguredPreference() {
        repository.saveOrUpdate(NotificationPreference.create("pref-1", "user-1", AlertType.BUDGET_THRESHOLD, NotificationChannel.EMAIL, false));

        GetNotificationPreferencesUseCase.Output output = useCase.execute(new GetNotificationPreferencesUseCase.Input("user-1"));

        List<PreferenceView> budgetEmail = output.preferences().stream()
                .filter(p -> p.alertType() == AlertType.BUDGET_THRESHOLD && p.channel() == NotificationChannel.EMAIL)
                .toList();
        assertThat(budgetEmail).hasSize(1);
        assertThat(budgetEmail.get(0).enabled()).isFalse();
    }

    @Test
    void doesNotMixPreferencesFromAnotherUser() {
        repository.saveOrUpdate(NotificationPreference.create("pref-1", "another-user", AlertType.BUDGET_THRESHOLD, NotificationChannel.EMAIL, false));

        GetNotificationPreferencesUseCase.Output output = useCase.execute(new GetNotificationPreferencesUseCase.Input("user-1"));

        assertThat(output.preferences()).allMatch(PreferenceView::enabled);
    }
}
