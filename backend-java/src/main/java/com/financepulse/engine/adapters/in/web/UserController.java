package com.financepulse.engine.adapters.in.web;

import com.financepulse.engine.adapters.in.web.dto.DeleteAccountRequest;
import com.financepulse.engine.application.usecases.user.DeleteAccountUseCase;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Rota protegida por {@link AuthenticationInterceptor}. RF-045/RF-007 — anonimização, não exclusão física (ver ADR-0023). */
@RestController
@RequestMapping("/users")
public class UserController {

    private final DeleteAccountUseCase deleteAccountUseCase;

    public UserController(DeleteAccountUseCase deleteAccountUseCase) {
        this.deleteAccountUseCase = deleteAccountUseCase;
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMe(@Valid @RequestBody DeleteAccountRequest request, HttpServletRequest httpRequest) {
        String userId = AuthenticationInterceptor.getAuthenticatedUserId(httpRequest);

        deleteAccountUseCase.execute(new DeleteAccountUseCase.Input(userId, request.password()));

        return ResponseEntity.noContent().build();
    }
}
