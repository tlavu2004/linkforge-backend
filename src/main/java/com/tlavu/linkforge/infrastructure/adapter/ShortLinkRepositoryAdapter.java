package com.tlavu.linkforge.infrastructure.adapter;

import com.tlavu.linkforge.domain.entity.ShortLink;
import com.tlavu.linkforge.domain.repository.ShortLinkRepository;
import com.tlavu.linkforge.domain.valueobject.ShortCode;
import com.tlavu.linkforge.infrastructure.persistence.entity.ShortLinkJpaEntity;
import com.tlavu.linkforge.infrastructure.persistence.mapper.ShortLinkMapper;
import com.tlavu.linkforge.infrastructure.persistence.repository.ShortLinkJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@SuppressWarnings("null")
public class ShortLinkRepositoryAdapter implements ShortLinkRepository {

    private final ShortLinkJpaRepository jpaRepository;
    private final ShortLinkMapper mapper;

    @Override
    public Optional<ShortLink> findByShortCode(ShortCode shortCode) {
        return jpaRepository.findByShortCode(shortCode.code()) // Use .code() accessor
                .map(mapper::toDomain);
    }

    @Override
    public ShortLink save(ShortLink shortLink) {
        ShortLinkJpaEntity entity = mapper.toJpaEntity(shortLink);
        ShortLinkJpaEntity savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<ShortLink> findById(Long id) {
        return jpaRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public void delete(Long id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public void incrementClickCount(ShortCode shortCode) {
        jpaRepository.incrementClickCountByShortCode(shortCode.code());
    }

    @Override
    public int deleteExpiredLinks(Instant now) {
        return jpaRepository.deleteByExpiresAtBefore(now);
    }

    @Override
    public Page<ShortLink> findByUserId(Long userId, Pageable pageable) {
        return jpaRepository.findByUserId(userId, pageable)
                .map(mapper::toDomain);
    }

    @Override
    public Page<ShortLink> findByUserIdAndKeyword(Long userId, String keyword, Pageable pageable) {
        return jpaRepository.findByUserIdAndKeyword(userId, keyword, pageable)
                .map(mapper::toDomain);
    }
}
