package com.tlavu.linkforge.domain.repository;

import com.tlavu.linkforge.domain.entity.PaymentTransaction;

import java.util.Optional;

public interface PaymentTransactionRepository {
    PaymentTransaction save(PaymentTransaction transaction);

    Optional<PaymentTransaction> findByOrderCode(String orderCode);
}
