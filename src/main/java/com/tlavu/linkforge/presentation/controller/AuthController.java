package com.tlavu.linkforge.presentation.controller;

import com.tlavu.linkforge.presentation.response.ApiResponse;
import com.tlavu.linkforge.application.dto.response.AuthResponse;
import com.tlavu.linkforge.application.dto.request.LoginRequest;
import com.tlavu.linkforge.application.dto.request.RegisterRequest;
import com.tlavu.linkforge.application.dto.request.VerifyEmailRequest;
import com.tlavu.linkforge.application.dto.request.ResendOtpRequest;
import com.tlavu.linkforge.application.dto.request.ForgotPasswordRequest;
import com.tlavu.linkforge.application.dto.request.ResetPasswordRequest;
import com.tlavu.linkforge.application.dto.request.TokenRefreshRequest;
import com.tlavu.linkforge.application.dto.request.LogoutRequest;
import com.tlavu.linkforge.application.dto.response.RegisterResponse;
import com.tlavu.linkforge.application.usecase.AuthUseCase;
import com.tlavu.linkforge.domain.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.tlavu.linkforge.shared.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Locale;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints for user registration, login, email verification, password reset, and logout")
@SuppressWarnings("null")
public class AuthController {

    private final AuthUseCase authUseCase;
    private final MessageService messageService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Creates a new user account and sends verification OTP to email")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(@RequestBody @Valid RegisterRequest request,
            Locale locale) {
        RegisterResponse response = authUseCase.register(request, locale);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse
                .success(messageService.getMessage("auth.register_success", locale), response));
    }

    @PostMapping("/verify-email")
    @Operation(summary = "Verify email", description = "Verifies email address using OTP sent during registration")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@RequestBody @Valid VerifyEmailRequest request, Locale locale) {
        authUseCase.verifyEmail(request);
        return ResponseEntity.ok(ApiResponse.success(messageService.getMessage("auth.verify_success", locale), null));
    }

    @PostMapping("/resend-otp")
    @Operation(summary = "Resend verification OTP", description = "Resends verification OTP to the user's email")
    public ResponseEntity<ApiResponse<Void>> resendOtp(@RequestBody @Valid ResendOtpRequest request, Locale locale) {
        authUseCase.resendVerificationOtp(request, locale);
        return ResponseEntity.ok(ApiResponse.success(messageService.getMessage("auth.resend_success", locale), null));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Forgot password", description = "Sends password reset OTP to the user's email")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@RequestBody @Valid ForgotPasswordRequest request,
            Locale locale) {
        authUseCase.forgotPassword(request, locale);
        return ResponseEntity.ok(ApiResponse.success(messageService.getMessage("auth.forgot_password_success", locale), null));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password", description = "Resets password using OTP and new password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@RequestBody @Valid ResetPasswordRequest request,
            Locale locale) {
        authUseCase.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success(messageService.getMessage("auth.reset_password_success"), null));
    }

    @PostMapping("/login")
    @Operation(summary = "Login an existing user", description = "Authenticates user and returns JWT + Refresh Token. Email must be verified first.")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@RequestBody @Valid LoginRequest request) {
        AuthResponse response = authUseCase.login(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token", description = "Exchanges a valid refresh token for a new JWT")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(@RequestBody @Valid TokenRefreshRequest request) {
        AuthResponse response = authUseCase.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout user", description = "Deletes the refresh token from the database")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestBody @Valid LogoutRequest request) {
        authUseCase.logout(request);
        return ResponseEntity.ok(ApiResponse.success(messageService.getMessage("auth.logout_success"), null));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get current user profile", description = "Returns the authenticated user's latest profile data")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCurrentUser(
            Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        User user = authUseCase.findUserByEmail(userDetails.getUsername());

        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("userId", user.getId());
        profile.put("name", user.getName());
        profile.put("email", user.getEmail());
        profile.put("role", user.getRole());
        profile.put("vip", user.isVipActive(Instant.now()));
        profile.put("vipExpiresAt", user.getVipExpiresAt());
        return ResponseEntity.ok(ApiResponse.success(profile));
    }
}
