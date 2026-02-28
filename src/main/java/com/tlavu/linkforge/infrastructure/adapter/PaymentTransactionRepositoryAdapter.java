package com.tlavu.linkforge.infrastructure.adapter;

import com.tlavu.linkforge.domain.entity.PaymentTransaction;
import com.tlavu.linkforge.domain.repository.PaymentTransactionRepository;
import com.tlavu.linkforge.infrastructure.persistence.mapper.PaymentTransactionMapper;
import com.tlavu.linkforge.infrastructure.persistence.repository.PaymentTransactionJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PaymentTransactionRepositoryAdapter implements PaymentTransactionRepository {

    private final PaymentTransactionJpaRepository jpaRepository;
    private final PaymentTransactionMapper mapper;

    @Override
    public PaymentTransaction save(PaymentTransaction transaction) {
        var entity = Objects.requireNonNull(mapper.toJpaEntity(transaction));
        var savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<PaymentTransaction> findByOrderCode(String orderCode) {
        return jpaRepository.findByOrderCode(orderCode)
                .map(mapper::toDomain);
    }
}
