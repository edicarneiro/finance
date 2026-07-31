package com.financepulse.engine.adapters.in.web;

import com.financepulse.engine.adapters.in.web.dto.LoginRequest;
import com.financepulse.engine.adapters.in.web.dto.LoginResponse;
import com.financepulse.engine.adapters.in.web.dto.RegisterRequest;
import com.financepulse.engine.adapters.in.web.dto.RegisterResponse;
import com.financepulse.engine.application.usecases.AuthenticateUserUseCase;
import com.financepulse.engine.application.usecases.RegisterUserUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final RegisterUserUseCase registerUserUseCase;
    private final AuthenticateUserUseCase authenticateUserUseCase;

    public AuthController(RegisterUserUseCase registerUserUseCase, AuthenticateUserUseCase authenticateUserUseCase) {
        this.registerUserUseCase = registerUserUseCase;
        this.authenticateUserUseCase = authenticateUserUseCase;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        RegisterUserUseCase.Output output = registerUserUseCase.execute(
                new RegisterUserUseCase.Input(request.email(), request.password()));
        return ResponseEntity.status(HttpStatus.CREATED).body(new RegisterResponse(output.userId()));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthenticateUserUseCase.Output output = authenticateUserUseCase.execute(
                new AuthenticateUserUseCase.Input(request.email(), request.password()));
        return ResponseEntity.ok(new LoginResponse(output.token()));
    }
}
