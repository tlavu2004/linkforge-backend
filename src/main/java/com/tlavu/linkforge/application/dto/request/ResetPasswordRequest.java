package com.tlavu.linkforge.application.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank(message = "Email is required") @Email String email,
        @NotBlank(message = "OTP is required") String otp,
        @NotBlank(message = "New password is required") @Size(min = 6, message = "Password must be at least 6 characters") String newPassword) {
}
