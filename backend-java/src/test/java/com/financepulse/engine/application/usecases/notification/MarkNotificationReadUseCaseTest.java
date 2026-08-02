package com.financepulse.engine.application.usecases.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.financepulse.engine.domain.notification.AlertType;
import com.financepulse.engine.domain.notification.Notification;
import com.financepulse.engine.domain.notification.NotificationChannel;
import com.financepulse.engine.domain.notification.errors.NotificationNotFoundException;
import com.financepulse.engine.testsupport.InMemoryNotificationRepository;
import java.util.EnumSet;
import org.junit.jupiter.api.Test;

class MarkNotificationReadUseCaseTest {

    private final InMemoryNotificationRepository repository = new InMemoryNotificationRepository();
    private final MarkNotificationReadUseCase useCase = new MarkNotificationReadUseCase(repository);

    @Test
    void marksAnExistingNotificationAsRead() {
        repository.save(Notification.create("n-1", "user-1", AlertType.BUDGET_THRESHOLD, "event:1", "msg", EnumSet.of(NotificationChannel.IN_APP)));

        useCase.execute(new MarkNotificationReadUseCase.Input("user-1", "n-1"));

        assertThat(repository.findByIdAndUserId("n-1", "user-1").orElseThrow().isRead()).isTrue();
    }

    @Test
    void rejectsMarkingANotificationBelongingToAnotherUser() {
        repository.save(Notification.create("n-1", "user-1", AlertType.BUDGET_THRESHOLD, "event:1", "msg", EnumSet.of(NotificationChannel.IN_APP)));

        assertThatThrownBy(() -> useCase.execute(new MarkNotificationReadUseCase.Input("another-user", "n-1")))
                .isInstanceOf(NotificationNotFoundException.class);
    }
}
