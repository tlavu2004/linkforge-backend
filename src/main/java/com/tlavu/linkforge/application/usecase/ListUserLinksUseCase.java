package com.tlavu.linkforge.application.usecase;

import com.tlavu.linkforge.application.dto.response.UserLinkResponse;
import com.tlavu.linkforge.domain.entity.ShortLink;
import com.tlavu.linkforge.domain.repository.ShortLinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ListUserLinksUseCase {

    private final ShortLinkRepository shortLinkRepository;

    public Page<UserLinkResponse> execute(Long userId, Pageable pageable) {
        return shortLinkRepository.findByUserId(userId, pageable)
                .map(this::toResponse);
    }

    private UserLinkResponse toResponse(ShortLink link) {
        return new UserLinkResponse(
                link.getShortCode().code(),
                link.getOriginalUrl().url(),
                link.getCreatedAt(),
                link.getExpiresAt(),
                link.getClickCount(),
                link.isExpired(Instant.now()));
    }
}
