package com.tlavu.linkforge.application.usecase;

import com.tlavu.linkforge.domain.entity.PaymentStatus;
import com.tlavu.linkforge.domain.entity.VipPackage;
import com.tlavu.linkforge.domain.repository.PaymentTransactionRepository;
import com.tlavu.linkforge.domain.repository.UserRepository;
import com.tlavu.linkforge.infrastructure.config.VNPayConfig;
import com.tlavu.linkforge.infrastructure.util.VNPayUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class HandlePaymentWebhookUseCaseImpl implements HandlePaymentWebhookUseCase {

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final UserRepository userRepository;
    private final VNPayConfig vnPayConfig;

    @Override
    @Transactional
    public void execute(Map<String, String> requestParams) {
        Map<String, String> fields = new HashMap<>();
        for (Map.Entry<String, String> entry : requestParams.entrySet()) {
            String fieldName = entry.getKey();
            String fieldValue = entry.getValue();
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                fields.put(fieldName, fieldValue);
            }
        }

        String vnp_SecureHash = fields.remove("vnp_SecureHash");
        fields.remove("vnp_SecureHashType"); // Sometimes present in old versions

        String signValue = VNPayUtil.hashAllFields(fields, vnPayConfig.getHashSecret());

        if (signValue.equals(vnp_SecureHash)) {
            String orderCode = fields.get("vnp_TxnRef");
            String responseCode = fields.get("vnp_ResponseCode");

            paymentTransactionRepository.findByOrderCode(orderCode).ifPresent(transaction -> {
                if (transaction.getStatus() == PaymentStatus.PENDING) {
                    if ("00".equals(responseCode)) {
                        // Success
                        transaction.markAsPaid(Instant.now());

                        // Grant VIP based on package
                        userRepository.findById(transaction.getUserId()).ifPresent(user -> {
                            try {
                                VipPackage vipPackage = VipPackage
                                        .fromCode(transaction.getPackageCode());

                                Instant expiration = Instant.now().plus(vipPackage.getDurationDuration(),
                                        vipPackage.getDurationUnit());
                                // if user is already VIP and expiration is further into the future, we extend
                                // it
                                if (user.isVipActive(Instant.now()) && user.getVipExpiresAt() != null) {
                                    expiration = user.getVipExpiresAt().plus(vipPackage.getDurationDuration(),
                                            vipPackage.getDurationUnit());
                                }
                                user.grantTemporaryVip(expiration);
                                log.info("Granted {} VIP to User ID: {}, expiring at: {}", vipPackage.name(),
                                        user.getId(), expiration);
                                userRepository.save(user);
                            } catch (Exception e) {
                                log.error("Failed to apply VIP package to user: {}", e.getMessage());
                            }
                        });

                    } else {
                        // Failed or cancelled
                        transaction.markAsCancelled();
                        log.info("Payment cancelled or failed for Order Code: {}", orderCode);
                    }
                    paymentTransactionRepository.save(transaction);
                } else {
                    log.warn("Transaction state is not PENDING for Order Code: {}. Current state: {}", orderCode,
                            transaction.getStatus());
                }
            });
        } else {
            log.error("Invalid Checksum for VNPay webhook. Potential spoofing attempt!");
            throw new IllegalArgumentException("payment.invalid_signature");
        }
    }
}
