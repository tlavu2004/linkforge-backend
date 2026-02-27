package com.tlavu.linkforge.presentation.dto;

import jakarta.validation.constraints.NotBlank;

public record VerifyAdTokenRequest(
        @NotBlank(message = "Token is required") String token,
        @NotBlank(message = "Short code is required") String shortCode) {
}
