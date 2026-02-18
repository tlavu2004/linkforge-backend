package com.tlavu.linkforge.infrastructure.persistence.repository;

import com.tlavu.linkforge.infrastructure.persistence.entity.ShortLinkJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShortLinkJpaRepository extends JpaRepository<ShortLinkJpaEntity, Long> {
    Optional<ShortLinkJpaEntity> findByCode(String code);
}
