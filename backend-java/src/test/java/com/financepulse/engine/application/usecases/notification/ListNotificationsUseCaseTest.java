package com.financepulse.engine.application.usecases.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.financepulse.engine.domain.notification.AlertType;
import com.financepulse.engine.domain.notification.Notification;
import com.financepulse.engine.domain.notification.NotificationChannel;
import com.financepulse.engine.testsupport.InMemoryNotificationRepository;
import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ListNotificationsUseCaseTest {

    private final InMemoryNotificationRepository repository = new InMemoryNotificationRepository();
    private final ListNotificationsUseCase useCase = new ListNotificationsUseCase(repository);

    @Test
    void onlyReturnsNotificationsDeliveredViaInApp() {
        repository.save(notification("n-1", "user-1", Set.of(NotificationChannel.IN_APP)));
        repository.save(notification("n-2", "user-1", Set.of(NotificationChannel.EMAIL)));

        ListNotificationsUseCase.Output output = useCase.execute(new ListNotificationsUseCase.Input("user-1"));

        assertThat(output.notifications()).extracting("id").containsExactly("n-1");
    }

    @Test
    void filtersToUnreadOnlyWhenRequested() {
        Notification unread = notification("n-1", "user-1", Set.of(NotificationChannel.IN_APP));
        Notification read = notification("n-2", "user-1", Set.of(NotificationChannel.IN_APP)).markRead();
        repository.save(unread);
        repository.save(read);

        ListNotificationsUseCase.Output output = useCase.execute(new ListNotificationsUseCase.Input("user-1", true));

        assertThat(output.notifications()).extracting("id").containsExactly("n-1");
    }

    @Test
    void doesNotMixNotificationsFromAnotherUser() {
        repository.save(notification("n-1", "another-user", Set.of(NotificationChannel.IN_APP)));

        ListNotificationsUseCase.Output output = useCase.execute(new ListNotificationsUseCase.Input("user-1"));

        assertThat(output.notifications()).isEmpty();
    }

    private static Notification notification(String id, String userId, Set<NotificationChannel> channels) {
        return Notification.create(id, userId, AlertType.BUDGET_THRESHOLD, "event:" + id, "mensagem", EnumSet.copyOf(channels));
    }
}
