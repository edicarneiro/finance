package com.financepulse.engine.application.services;

import static org.assertj.core.api.Assertions.assertThat;

import com.financepulse.engine.domain.notification.AlertType;
import com.financepulse.engine.domain.notification.NotificationChannel;
import com.financepulse.engine.domain.notification.NotificationPreference;
import java.util.List;
import org.junit.jupiter.api.Test;

class NotificationPreferenceResolverTest {

    @Test
    void defaultsToEnabledWhenNoPreferenceIsConfigured() {
        boolean enabled = NotificationPreferenceResolver.isEnabled(List.of(), AlertType.BUDGET_THRESHOLD, NotificationChannel.EMAIL);

        assertThat(enabled).isTrue();
    }

    @Test
    void usesTheExplicitlyConfiguredValueWhenPresent() {
        List<NotificationPreference> preferences =
                List.of(NotificationPreference.create("pref-1", "user-1", AlertType.BUDGET_THRESHOLD, NotificationChannel.EMAIL, false));

        boolean enabled = NotificationPreferenceResolver.isEnabled(preferences, AlertType.BUDGET_THRESHOLD, NotificationChannel.EMAIL);

        assertThat(enabled).isFalse();
    }

    @Test
    void doesNotConfuseDifferentAlertTypesOrChannels() {
        List<NotificationPreference> preferences =
                List.of(NotificationPreference.create("pref-1", "user-1", AlertType.BUDGET_THRESHOLD, NotificationChannel.EMAIL, false));

        assertThat(NotificationPreferenceResolver.isEnabled(preferences, AlertType.BUDGET_THRESHOLD, NotificationChannel.IN_APP)).isTrue();
        assertThat(NotificationPreferenceResolver.isEnabled(preferences, AlertType.GOAL_THRESHOLD, NotificationChannel.EMAIL)).isTrue();
    }
}
