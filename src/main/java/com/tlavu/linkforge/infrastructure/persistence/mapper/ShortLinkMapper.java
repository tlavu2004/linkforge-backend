package com.tlavu.linkforge.infrastructure.persistence.mapper;

import com.tlavu.linkforge.domain.entity.ShortLink;
import com.tlavu.linkforge.domain.valueobject.OriginalUrl;
import com.tlavu.linkforge.domain.valueobject.ShortCode;
import com.tlavu.linkforge.infrastructure.persistence.entity.ShortLinkJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface ShortLinkMapper {

    @Mapping(target = "shortCode", source = "shortCode", qualifiedByName = "toShortCode")
    @Mapping(target = "originalUrl", source = "originalUrl", qualifiedByName = "toOriginalUrl")
    @Mapping(target = "deleteTokenHash", source = "deleteTokenHash")
    ShortLink toDomain(ShortLinkJpaEntity entity);

    @Mapping(target = "shortCode", source = "shortCode.code")
    @Mapping(target = "originalUrl", source = "originalUrl.url")
    ShortLinkJpaEntity toJpaEntity(ShortLink domain);

    @Named("toShortCode")
    default ShortCode toShortCode(String code) {
        if (code == null)
            return null;
        return ShortCode.of(code);
    }

    @Named("toOriginalUrl")
    default OriginalUrl toOriginalUrl(String url) {
        if (url == null)
            return null;
        return OriginalUrl.of(url);
    }
}
