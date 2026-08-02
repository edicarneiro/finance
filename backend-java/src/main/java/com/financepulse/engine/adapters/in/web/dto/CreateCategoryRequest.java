package com.financepulse.engine.adapters.in.web.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateCategoryRequest(@NotBlank String name, String parentCategoryId) {
}
