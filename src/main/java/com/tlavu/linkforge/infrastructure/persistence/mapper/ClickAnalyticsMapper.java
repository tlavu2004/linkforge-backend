package com.tlavu.linkforge.infrastructure.persistence.mapper;

import com.tlavu.linkforge.domain.entity.ClickAnalytics;
import com.tlavu.linkforge.infrastructure.persistence.entity.ClickAnalyticsJpaEntity;
import org.mapstruct.Mapper;
import org.springframework.lang.NonNull;

@Mapper(componentModel = "spring")
public interface ClickAnalyticsMapper {
    @NonNull
    ClickAnalyticsJpaEntity toJpaEntity(@NonNull ClickAnalytics domain);
    @NonNull
    ClickAnalytics toDomain(@NonNull ClickAnalyticsJpaEntity jpaEntity);
}
