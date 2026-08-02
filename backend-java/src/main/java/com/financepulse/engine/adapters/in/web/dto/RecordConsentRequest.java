package com.financepulse.engine.adapters.in.web.dto;

import jakarta.validation.constraints.NotBlank;

public record RecordConsentRequest(@NotBlank String version) {
}
