package com.financepulse.engine.adapters.in.web;

import com.financepulse.engine.adapters.in.web.dto.CheckedNotificationResponse;
import com.financepulse.engine.adapters.in.web.dto.NotificationPreferenceResponse;
import com.financepulse.engine.adapters.in.web.dto.NotificationResponse;
import com.financepulse.engine.adapters.in.web.dto.UpdateNotificationPreferenceRequest;
import com.financepulse.engine.application.usecases.notification.CheckNotificationsUseCase;
import com.financepulse.engine.application.usecases.notification.GetNotificationPreferencesUseCase;
import com.financepulse.engine.application.usecases.notification.ListNotificationsUseCase;
import com.financepulse.engine.application.usecases.notification.MarkNotificationReadUseCase;
import com.financepulse.engine.application.usecases.notification.UpdateNotificationPreferencesUseCase;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Rotas protegidas por {@link AuthenticationInterceptor}. RF-040 a RF-042; RF-043 fora do escopo (ver ADR-0022). */
@RestController
public class NotificationController {

    private final GetNotificationPreferencesUseCase getNotificationPreferencesUseCase;
    private final UpdateNotificationPreferencesUseCase updateNotificationPreferencesUseCase;
    private final CheckNotificationsUseCase checkNotificationsUseCase;
    private final ListNotificationsUseCase listNotificationsUseCase;
    private final MarkNotificationReadUseCase markNotificationReadUseCase;

    public NotificationController(
            GetNotificationPreferencesUseCase getNotificationPreferencesUseCase,
            UpdateNotificationPreferencesUseCase updateNotificationPreferencesUseCase,
            CheckNotificationsUseCase checkNotificationsUseCase,
            ListNotificationsUseCase listNotificationsUseCase,
            MarkNotificationReadUseCase markNotificationReadUseCase) {
        this.getNotificationPreferencesUseCase = getNotificationPreferencesUseCase;
        this.updateNotificationPreferencesUseCase = updateNotificationPreferencesUseCase;
        this.checkNotificationsUseCase = checkNotificationsUseCase;
        this.listNotificationsUseCase = listNotificationsUseCase;
        this.markNotificationReadUseCase = markNotificationReadUseCase;
    }

    @GetMapping("/notification-preferences")
    public ResponseEntity<List<NotificationPreferenceResponse>> getPreferences(HttpServletRequest httpRequest) {
        String userId = AuthenticationInterceptor.getAuthenticatedUserId(httpRequest);

        GetNotificationPreferencesUseCase.Output output = getNotificationPreferencesUseCase.execute(new GetNotificationPreferencesUseCase.Input(userId));

        return ResponseEntity.ok(output.preferences().stream().map(NotificationPreferenceResponse::from).toList());
    }

    @PutMapping("/notification-preferences")
    public ResponseEntity<Void> updatePreferences(
            @Valid @RequestBody List<UpdateNotificationPreferenceRequest> requests, HttpServletRequest httpRequest) {
        String userId = AuthenticationInterceptor.getAuthenticatedUserId(httpRequest);

        List<UpdateNotificationPreferencesUseCase.PreferenceUpdate> updates =
                requests.stream().map(request -> new UpdateNotificationPreferencesUseCase.PreferenceUpdate(
                        request.alertType(), request.channel(), request.enabled())).toList();
        updateNotificationPreferencesUseCase.execute(new UpdateNotificationPreferencesUseCase.Input(userId, updates));

        return ResponseEntity.ok().build();
    }

    @PostMapping("/notifications/check")
    public ResponseEntity<List<CheckedNotificationResponse>> check(HttpServletRequest httpRequest) {
        String userId = AuthenticationInterceptor.getAuthenticatedUserId(httpRequest);

        CheckNotificationsUseCase.Output output = checkNotificationsUseCase.execute(new CheckNotificationsUseCase.Input(userId));

        return ResponseEntity.ok(output.newNotifications().stream().map(CheckedNotificationResponse::from).toList());
    }

    @GetMapping("/notifications")
    public ResponseEntity<List<NotificationResponse>> list(
            @RequestParam(name = "unreadOnly", defaultValue = "false") boolean unreadOnly, HttpServletRequest httpRequest) {
        String userId = AuthenticationInterceptor.getAuthenticatedUserId(httpRequest);

        ListNotificationsUseCase.Output output = listNotificationsUseCase.execute(new ListNotificationsUseCase.Input(userId, unreadOnly));

        return ResponseEntity.ok(output.notifications().stream().map(NotificationResponse::from).toList());
    }

    @PutMapping("/notifications/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable String id, HttpServletRequest httpRequest) {
        String userId = AuthenticationInterceptor.getAuthenticatedUserId(httpRequest);

        markNotificationReadUseCase.execute(new MarkNotificationReadUseCase.Input(userId, id));

        return ResponseEntity.ok().build();
    }
}
