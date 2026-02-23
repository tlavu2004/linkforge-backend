package com.tlavu.linkforge.application.dto;

import jakarta.validation.constraints.NotBlank;

public record TokenRefreshRequest(
        @NotBlank(message = "Refresh token is required") String refreshToken) {
}
