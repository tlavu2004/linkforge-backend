package com.tlavu.linkforge.infrastructure.persistence.entity;

import com.tlavu.linkforge.domain.entity.PaymentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "payment_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentTransactionJpaEntity {

    @Id
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "order_code", nullable = false, unique = true, length = 50)
    private String orderCode;

    @Column(nullable = false)
    private int amount;

    @Column(name = "package_code", length = 50)
    private String packageCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "paid_at")
    private Instant paidAt;
}
