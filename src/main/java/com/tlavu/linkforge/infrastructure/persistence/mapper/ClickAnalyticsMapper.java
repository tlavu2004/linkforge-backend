package com.tlavu.linkforge.infrastructure.persistence.mapper;

import com.tlavu.linkforge.domain.entity.ClickAnalytics;
import com.tlavu.linkforge.infrastructure.persistence.entity.ClickAnalyticsJpaEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ClickAnalyticsMapper {

    ClickAnalyticsJpaEntity toJpaEntity(ClickAnalytics domain);

    ClickAnalytics toDomain(ClickAnalyticsJpaEntity jpaEntity);
}
