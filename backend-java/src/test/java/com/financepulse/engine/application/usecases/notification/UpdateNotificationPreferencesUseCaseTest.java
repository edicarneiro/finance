package com.financepulse.engine.application.usecases.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.financepulse.engine.application.usecases.notification.UpdateNotificationPreferencesUseCase.PreferenceUpdate;
import com.financepulse.engine.domain.notification.AlertType;
import com.financepulse.engine.domain.notification.NotificationChannel;
import com.financepulse.engine.testsupport.InMemoryNotificationPreferenceRepository;
import com.financepulse.engine.testsupport.SequentialIdGenerator;
import java.util.List;
import org.junit.jupiter.api.Test;

class UpdateNotificationPreferencesUseCaseTest {

    private final InMemoryNotificationPreferenceRepository repository = new InMemoryNotificationPreferenceRepository();
    private final UpdateNotificationPreferencesUseCase useCase =
            new UpdateNotificationPreferencesUseCase(repository, new SequentialIdGenerator("pref"));

    @Test
    void persistsOnlyTheInformedCombinations() {
        useCase.execute(new UpdateNotificationPreferencesUseCase.Input(
                "user-1", List.of(new PreferenceUpdate(AlertType.BUDGET_THRESHOLD, NotificationChannel.EMAIL, false))));

        assertThat(repository.findAllByUserId("user-1")).hasSize(1);
        assertThat(repository.findAllByUserId("user-1").get(0).isEnabled()).isFalse();
    }

    @Test
    void updatingTheSameCombinationTwiceOverwritesRatherThanDuplicates() {
        useCase.execute(new UpdateNotificationPreferencesUseCase.Input(
                "user-1", List.of(new PreferenceUpdate(AlertType.BUDGET_THRESHOLD, NotificationChannel.EMAIL, false))));
        useCase.execute(new UpdateNotificationPreferencesUseCase.Input(
                "user-1", List.of(new PreferenceUpdate(AlertType.BUDGET_THRESHOLD, NotificationChannel.EMAIL, true))));

        assertThat(repository.findAllByUserId("user-1")).hasSize(1);
        assertThat(repository.findAllByUserId("user-1").get(0).isEnabled()).isTrue();
    }
}
