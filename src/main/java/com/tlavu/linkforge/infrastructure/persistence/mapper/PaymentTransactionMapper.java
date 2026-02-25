package com.tlavu.linkforge.infrastructure.persistence.mapper;

import com.tlavu.linkforge.domain.entity.PaymentTransaction;
import com.tlavu.linkforge.infrastructure.persistence.entity.PaymentTransactionJpaEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PaymentTransactionMapper {
    PaymentTransactionJpaEntity toJpaEntity(PaymentTransaction domain);

    PaymentTransaction toDomain(PaymentTransactionJpaEntity jpaEntity);
}
