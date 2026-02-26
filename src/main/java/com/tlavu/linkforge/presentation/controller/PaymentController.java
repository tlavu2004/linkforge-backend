package com.tlavu.linkforge.presentation.controller;

import com.tlavu.linkforge.presentation.response.ApiResponse;
import com.tlavu.linkforge.application.usecase.CreatePaymentLinkUseCase;
import com.tlavu.linkforge.application.usecase.HandlePaymentWebhookUseCase;
import com.tlavu.linkforge.domain.repository.UserRepository;
import com.tlavu.linkforge.infrastructure.util.VNPayUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payment Management", description = "Endpoints for handling VIP payments via VNPay")
public class PaymentController {

    private final CreatePaymentLinkUseCase createPaymentLinkUseCase;
    private final HandlePaymentWebhookUseCase handlePaymentWebhookUseCase;
    private final UserRepository userRepository;

    @PostMapping("/vip-upgrade")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Create VIP Upgrade Payment Link", description = "Initiates a VNPay transaction and returns the checkout URL")
    public ResponseEntity<com.tlavu.linkforge.presentation.response.ApiResponse<String>> createVipUpgradeLink(
            @jakarta.validation.Valid @RequestBody com.tlavu.linkforge.application.dto.CreatePaymentLinkRequest requestBody,
            HttpServletRequest request,
            Authentication authentication) {

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        com.tlavu.linkforge.domain.entity.User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (user.getRole() == com.tlavu.linkforge.domain.entity.Role.ADMIN) {
            throw new IllegalArgumentException("Admins do not need to purchase VIP packages.");
        }

        if (user.isVipActive(java.time.Instant.now()) && user.getVipExpiresAt() == null) {
            throw new IllegalArgumentException("You already have Lifetime VIP. No need to purchase again.");
        }

        Long userId = user.getId();

        String ipAddress = VNPayUtil.getIpAddress(request);

        String paymentUrl = createPaymentLinkUseCase.execute(userId, requestBody.packageCode(), ipAddress);

        return ResponseEntity.ok(ApiResponse.success("Payment link generated", paymentUrl));
    }

    @GetMapping("/vnpay-return")
    @Operation(summary = "VNPay Return Callback", description = "Handles the synchronous redirect callback from VNPay after payment")
    public ResponseEntity<Void> vnpayReturn(HttpServletRequest request) {
        Map<String, String> fields = new HashMap<>();
        for (Map.Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
            fields.put(entry.getKey(), entry.getValue()[0]);
        }

        try {
            handlePaymentWebhookUseCase.execute(fields);
        } catch (Exception e) {
            // Logged in use case, redirect to frontend failure page
            return ResponseEntity.status(302)
                    .location(java.util.Objects.requireNonNull(URI.create("http://localhost:5173/payment-failure")))
                    .build();
        }

        return ResponseEntity.status(302)
                .location(java.util.Objects.requireNonNull(URI.create("http://localhost:5173/payment-success")))
                .build();
    }
}
