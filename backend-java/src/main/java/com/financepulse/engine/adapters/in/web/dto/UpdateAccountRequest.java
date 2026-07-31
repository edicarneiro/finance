package com.financepulse.engine.adapters.in.web.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateAccountRequest(@NotBlank String name) {
}
