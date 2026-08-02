package com.financepulse.engine.adapters.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.financepulse.engine.domain.notification.AlertType;
import com.financepulse.engine.domain.notification.NotificationChannel;
import com.financepulse.engine.domain.notification.NotificationPreference;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

/** Exercita o adaptador contra H2 real, mesma disciplina dos demais adaptadores de persistência (rules.md § 3). */
@DataJpaTest
class JpaNotificationPreferenceRepositoryAdapterTest {

    @Autowired
    private SpringDataNotificationPreferenceJpaRepository jpaRepository;

    @Test
    void savesAndReloadsAPreference() {
        JpaNotificationPreferenceRepositoryAdapter adapter = new JpaNotificationPreferenceRepositoryAdapter(jpaRepository);

        adapter.saveOrUpdate(NotificationPreference.create("pref-1", "user-1", AlertType.BUDGET_THRESHOLD, NotificationChannel.EMAIL, false));

        List<NotificationPreference> found = adapter.findAllByUserId("user-1");
        assertThat(found).hasSize(1);
        assertThat(found.get(0).isEnabled()).isFalse();
    }

    @Test
    void upsertsInPlaceInsteadOfCreatingASecondRowForTheSameCombination() {
        JpaNotificationPreferenceRepositoryAdapter adapter = new JpaNotificationPreferenceRepositoryAdapter(jpaRepository);

        adapter.saveOrUpdate(NotificationPreference.create("pref-1", "user-1", AlertType.BUDGET_THRESHOLD, NotificationChannel.EMAIL, false));
        adapter.saveOrUpdate(NotificationPreference.create("pref-2", "user-1", AlertType.BUDGET_THRESHOLD, NotificationChannel.EMAIL, true));

        List<NotificationPreference> found = adapter.findAllByUserId("user-1");
        assertThat(found).hasSize(1);
        assertThat(found.get(0).isEnabled()).isTrue();
    }

    @Test
    void doesNotMixPreferencesFromAnotherUser() {
        JpaNotificationPreferenceRepositoryAdapter adapter = new JpaNotificationPreferenceRepositoryAdapter(jpaRepository);
        adapter.saveOrUpdate(NotificationPreference.create("pref-1", "user-1", AlertType.BUDGET_THRESHOLD, NotificationChannel.EMAIL, false));

        assertThat(adapter.findAllByUserId("another-user")).isEmpty();
    }
}
