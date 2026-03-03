package com.tlavu.linkforge.presentation.controller;

import com.tlavu.linkforge.presentation.response.ApiResponse;
import com.tlavu.linkforge.application.usecase.CreatePaymentLinkUseCase;
import com.tlavu.linkforge.application.usecase.HandlePaymentWebhookUseCase;
import com.tlavu.linkforge.application.dto.request.CreatePaymentLinkRequest;
import com.tlavu.linkforge.domain.entity.Role;
import com.tlavu.linkforge.domain.entity.User;
import com.tlavu.linkforge.domain.repository.UserRepository;
import com.tlavu.linkforge.infrastructure.util.VNPayUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${application.frontend.url}")
    private String frontendUrl;

    @PostMapping("/vip-upgrade")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Create VIP Upgrade Payment Link", description = "Initiates a VNPay transaction and returns the checkout URL")
    public ResponseEntity<ApiResponse<String>> createVipUpgradeLink(
            @Valid @RequestBody CreatePaymentLinkRequest requestBody,
            HttpServletRequest request,
            Authentication authentication) {

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (user.getRole() == Role.ADMIN) {
            throw new IllegalArgumentException("Admins do not need to purchase VIP packages.");
        }

        if (user.isVipActive(Instant.now()) && user.getVipExpiresAt() == null) {
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

        // Check VNPay response code first — "00" means success
        String responseCode = fields.get("vnp_ResponseCode");
        if (!"00".equals(responseCode)) {
            // Payment was cancelled or failed — still process to update transaction status
            try {
                handlePaymentWebhookUseCase.execute(fields);
            } catch (Exception e) {
                // Log but still redirect to failure
            }
            return ResponseEntity.status(302)
                    .location(Objects.requireNonNull(URI.create(frontendUrl + "/vip-upgrade")))
                    .build();
        }

        try {
            handlePaymentWebhookUseCase.execute(fields);
        } catch (Exception e) {
            return ResponseEntity.status(302)
                    .location(Objects.requireNonNull(URI.create(frontendUrl + "/vip-upgrade")))
                    .build();
        }

        return ResponseEntity.status(302)
                .location(Objects.requireNonNull(URI.create(frontendUrl + "/payment-success")))
                .build();
    }
}
