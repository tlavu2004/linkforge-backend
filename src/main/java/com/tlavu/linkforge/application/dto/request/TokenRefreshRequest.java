package com.tlavu.linkforge.application.dto.request;

import jakarta.validation.constraints.NotBlank;

public record TokenRefreshRequest(
        @NotBlank(message = "Refresh token is required") String refreshToken) {
}
