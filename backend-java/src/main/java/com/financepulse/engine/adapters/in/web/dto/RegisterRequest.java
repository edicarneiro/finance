package com.financepulse.engine.adapters.in.web.dto;

import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(@NotBlank String email, @NotBlank String password) {
}
