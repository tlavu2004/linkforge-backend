package com.tlavu.linkforge.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@Getter
@AllArgsConstructor
public class PaymentTransaction {
    private final Long id;
    private final Long userId;
    private final String orderCode;
    private final int amount;
    private PaymentStatus status;
    private final Instant createdAt;
    private Instant paidAt;

    public static PaymentTransaction create(Long id, Long userId, String orderCode, int amount) {
        return new PaymentTransaction(id, userId, orderCode, amount, PaymentStatus.PENDING, Instant.now(), null);
    }

    public void markAsPaid(Instant paidAt) {
        this.status = PaymentStatus.PAID;
        this.paidAt = paidAt;
    }

    public void markAsCancelled() {
        this.status = PaymentStatus.CANCELLED;
    }
}
