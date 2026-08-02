package com.financepulse.engine.adapters.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.financepulse.engine.domain.notification.AlertType;
import com.financepulse.engine.domain.notification.Notification;
import com.financepulse.engine.domain.notification.NotificationChannel;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

/** Exercita o adaptador contra H2 real, mesma disciplina dos demais adaptadores de persistência (rules.md § 3). */
@DataJpaTest
class JpaNotificationRepositoryAdapterTest {

    @Autowired
    private SpringDataNotificationJpaRepository jpaRepository;

    @Test
    void savesAndReloadsANotificationWithDeliveredChannels() {
        JpaNotificationRepositoryAdapter adapter = new JpaNotificationRepositoryAdapter(jpaRepository);
        Notification notification = Notification.create(
                "notif-1", "user-1", AlertType.BUDGET_THRESHOLD, "budget:budget-1:period:2026-07-01:threshold:80", "Orçamento estourado",
                Set.of(NotificationChannel.IN_APP, NotificationChannel.EMAIL));

        adapter.save(notification);

        Optional<Notification> found = adapter.findByIdAndUserId("notif-1", "user-1");
        assertThat(found).isPresent();
        assertThat(found.get().getMessage()).isEqualTo("Orçamento estourado");
        assertThat(found.get().getDeliveredChannels()).containsExactlyInAnyOrder(NotificationChannel.IN_APP, NotificationChannel.EMAIL);
        assertThat(found.get().isRead()).isFalse();
    }

    @Test
    void toleratesAnEmptyDeliveredChannelsSet() {
        JpaNotificationRepositoryAdapter adapter = new JpaNotificationRepositoryAdapter(jpaRepository);
        Notification notification =
                Notification.create("notif-1", "user-1", AlertType.ATYPICAL_SPENDING, "transaction:tx-1:atypical", "msg", EnumSet.noneOf(
                        NotificationChannel.class));

        adapter.save(notification);

        assertThat(adapter.findByIdAndUserId("notif-1", "user-1").orElseThrow().getDeliveredChannels()).isEmpty();
    }

    @Test
    void detectsAnExistingEventKeyForDeduplication() {
        JpaNotificationRepositoryAdapter adapter = new JpaNotificationRepositoryAdapter(jpaRepository);
        adapter.save(Notification.create("notif-1", "user-1", AlertType.GOAL_THRESHOLD, "goal:goal-1:achieved", "msg", Set.of(NotificationChannel.IN_APP)));

        assertThat(adapter.existsByUserIdAndEventKey("user-1", "goal:goal-1:achieved")).isTrue();
        assertThat(adapter.existsByUserIdAndEventKey("user-1", "goal:goal-2:achieved")).isFalse();
        assertThat(adapter.existsByUserIdAndEventKey("another-user", "goal:goal-1:achieved")).isFalse();
    }

    @Test
    void listsAllNotificationsForAUser() {
        JpaNotificationRepositoryAdapter adapter = new JpaNotificationRepositoryAdapter(jpaRepository);
        adapter.save(Notification.create("notif-1", "user-1", AlertType.BUDGET_THRESHOLD, "event-1", "msg", Set.of(NotificationChannel.IN_APP)));
        adapter.save(Notification.create("notif-2", "user-1", AlertType.GOAL_THRESHOLD, "event-2", "msg", Set.of(NotificationChannel.IN_APP)));
        adapter.save(Notification.create("notif-3", "user-2", AlertType.BUDGET_THRESHOLD, "event-3", "msg", Set.of(NotificationChannel.IN_APP)));

        List<Notification> notifications = adapter.findAllByUserId("user-1");

        assertThat(notifications).hasSize(2).extracting(Notification::getId).containsExactlyInAnyOrder("notif-1", "notif-2");
    }

    @Test
    void persistsMarkingANotificationAsRead() {
        JpaNotificationRepositoryAdapter adapter = new JpaNotificationRepositoryAdapter(jpaRepository);
        Notification notification =
                Notification.create("notif-1", "user-1", AlertType.BUDGET_THRESHOLD, "event-1", "msg", Set.of(NotificationChannel.IN_APP));
        adapter.save(notification);

        adapter.update(notification.markRead());

        assertThat(adapter.findByIdAndUserId("notif-1", "user-1").orElseThrow().isRead()).isTrue();
    }
}
