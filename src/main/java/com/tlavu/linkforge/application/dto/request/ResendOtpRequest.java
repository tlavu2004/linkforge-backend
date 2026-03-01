package com.tlavu.linkforge.application.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ResendOtpRequest(
        @NotBlank(message = "Email is required") @Email String email) {
}
