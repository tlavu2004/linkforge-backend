package com.tlavu.linkforge.presentation.controller;

import com.tlavu.linkforge.application.usecase.VerifyAdTokenUseCase;
import com.tlavu.linkforge.presentation.response.ApiResponse;
import com.tlavu.linkforge.presentation.dto.VerifyAdTokenRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/v1/ads")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Advertisements", description = "Endpoints for handling ad token verification")
public class AdController {

    private final VerifyAdTokenUseCase verifyAdTokenUseCase;

    @Operation(summary = "Verify Ad Token", description = "Verifies the ad token and returns the original URL if 5 seconds have passed.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Token verified successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid or expired token, or wait time not met")
    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<String>> verifyAdToken(@Valid @RequestBody VerifyAdTokenRequest request, HttpServletRequest httpRequest) {
        log.info("Received request to verify ad token for shortCode: {}", request.shortCode());
        String ipAddress = httpRequest.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty()) {
            ipAddress = httpRequest.getRemoteAddr();
        } else {
            ipAddress = ipAddress.split(",")[0].trim();
        }

        String userAgent = httpRequest.getHeader("User-Agent");
        String referrer = httpRequest.getHeader("Referer");

        String originalUrl = verifyAdTokenUseCase.execute(request.token(), request.shortCode(), ipAddress, userAgent, referrer);
        log.info("Successfully verified ad token for shortCode: {}", request.shortCode());
        return ResponseEntity.ok(ApiResponse.success("Token verified successfully", originalUrl));
    }
}
