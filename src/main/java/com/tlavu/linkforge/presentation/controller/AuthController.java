package com.tlavu.linkforge.presentation.controller;

import com.tlavu.linkforge.presentation.response.ApiResponse;
import com.tlavu.linkforge.application.dto.response.AuthResponse;
import com.tlavu.linkforge.application.dto.request.LoginRequest;
import com.tlavu.linkforge.application.dto.request.RegisterRequest;
import com.tlavu.linkforge.application.dto.request.VerifyEmailRequest;
import com.tlavu.linkforge.application.dto.request.ResendOtpRequest;
import com.tlavu.linkforge.application.dto.request.ForgotPasswordRequest;
import com.tlavu.linkforge.application.dto.request.ResetPasswordRequest;
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
import io.swagger.v3.oas.annotations.tags.Tag;

import com.tlavu.linkforge.application.dto.request.TokenRefreshRequest;
import com.tlavu.linkforge.application.dto.request.LogoutRequest;
import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints for user registration, login, email verification, password reset, and logout")
public class AuthController {

    private final AuthUseCase authUseCase;

    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Creates a new user account and sends verification OTP to email")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(@RequestBody @Valid RegisterRequest request) {
        RegisterResponse response = authUseCase.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse
                .success("Registration successful. Please check your email for verification code.", response));
    }

    @PostMapping("/verify-email")
    @Operation(summary = "Verify email", description = "Verifies email address using OTP sent during registration")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@RequestBody @Valid VerifyEmailRequest request) {
        authUseCase.verifyEmail(request);
        return ResponseEntity.ok(ApiResponse.success("Email verified successfully", null));
    }

    @PostMapping("/resend-otp")
    @Operation(summary = "Resend verification OTP", description = "Resends verification OTP to the user's email")
    public ResponseEntity<ApiResponse<Void>> resendOtp(@RequestBody @Valid ResendOtpRequest request) {
        authUseCase.resendVerificationOtp(request);
        return ResponseEntity.ok(ApiResponse.success("Verification code sent", null));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Forgot password", description = "Sends password reset OTP to the user's email")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@RequestBody @Valid ForgotPasswordRequest request) {
        authUseCase.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password reset code sent to your email", null));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password", description = "Resets password using OTP and new password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@RequestBody @Valid ResetPasswordRequest request) {
        authUseCase.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password reset successfully", null));
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
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully", null));
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
