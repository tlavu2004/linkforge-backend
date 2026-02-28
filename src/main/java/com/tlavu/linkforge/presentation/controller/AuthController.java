package com.tlavu.linkforge.presentation.controller;

import com.tlavu.linkforge.presentation.response.ApiResponse;
import com.tlavu.linkforge.application.dto.response.AuthResponse;
import com.tlavu.linkforge.application.dto.request.LoginRequest;
import com.tlavu.linkforge.application.dto.request.RegisterRequest;
import com.tlavu.linkforge.application.dto.response.RegisterResponse;
import com.tlavu.linkforge.application.usecase.AuthUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
@Tag(name = "Authentication", description = "Endpoints for user registration, login, refresh tokens, and logout")
public class AuthController {

    private final AuthUseCase authUseCase;

    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Creates a new user account without tokens")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(@RequestBody @Valid RegisterRequest request) {
        RegisterResponse response = authUseCase.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PostMapping("/login")
    @Operation(summary = "Login an existing user", description = "Authenticates user and returns JWT + Refresh Token")
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
}
