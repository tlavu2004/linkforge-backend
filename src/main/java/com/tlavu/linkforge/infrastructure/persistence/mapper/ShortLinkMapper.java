package com.tlavu.linkforge.infrastructure.persistence.mapper;

import com.tlavu.linkforge.domain.entity.ShortLink;
import com.tlavu.linkforge.domain.valueobject.OriginalUrl;
import com.tlavu.linkforge.domain.valueobject.ShortCode;
import com.tlavu.linkforge.infrastructure.persistence.entity.ShortLinkJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class ShortLinkMapper {

    public ShortLink toDomain(ShortLinkJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return new ShortLink(
                entity.getId(),
                ShortCode.of(entity.getCode()),
                OriginalUrl.of(entity.getOriginalUrl()),
                entity.getCreatedAt(),
                entity.getExpiresAt(),
                entity.getIsActive(),
                entity.getClickCount(),
                entity.getDeleteTokenHash());
    }

    public ShortLinkJpaEntity toJpaEntity(ShortLink domain) {
        if (domain == null) {
            return null;
        }
        return new ShortLinkJpaEntity(
                domain.getId(),
                domain.getShortCode().code(),
                domain.getOriginalUrl().url(),
                domain.getCreatedAt(),
                domain.getExpiresAt(),
                domain.getClickCount(),
                domain.isEnabled(),
                domain.getDeleteTokenHash());
    }
}
