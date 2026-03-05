package com.tlavu.linkforge.infrastructure.persistence.mapper;

import com.tlavu.linkforge.domain.entity.User;
import com.tlavu.linkforge.infrastructure.persistence.entity.UserJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "email", source = "email")
    @Mapping(target = "passwordHash", source = "passwordHash")
    @Mapping(target = "role", source = "role")
    @Mapping(target = "emailVerified", source = "emailVerified")
    @Mapping(target = "vip", source = "vip")
    @Mapping(target = "vipExpiresAt", source = "vipExpiresAt")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", source = "updatedAt")
    User toDomain(UserJpaEntity entity);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "email", source = "email")
    @Mapping(target = "passwordHash", source = "passwordHash")
    @Mapping(target = "role", source = "role")
    @Mapping(target = "emailVerified", source = "emailVerified")
    @Mapping(target = "vip", source = "vip")
    @Mapping(target = "vipExpiresAt", source = "vipExpiresAt")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", source = "updatedAt")
    UserJpaEntity toJpaEntity(User domain);
}
