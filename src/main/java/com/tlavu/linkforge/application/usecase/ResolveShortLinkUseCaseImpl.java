package com.tlavu.linkforge.application.usecase;

import com.tlavu.linkforge.application.dto.ShortLinkResponse;
import com.tlavu.linkforge.domain.entity.ShortLink;
import com.tlavu.linkforge.domain.exception.ShortLinkExpiredException;
import com.tlavu.linkforge.domain.exception.ShortLinkNotFoundException;
import com.tlavu.linkforge.domain.repository.ShortLinkRepository;
import com.tlavu.linkforge.domain.valueobject.ShortCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ResolveShortLinkUseCaseImpl implements ResolveShortLinkUseCase {

    private final ShortLinkRepository shortLinkRepository;

    @Override
    @Transactional(readOnly = true)
    public ShortLinkResponse execute(String shortCode) {
        ShortCode code = ShortCode.of(shortCode);

        ShortLink shortLink = shortLinkRepository.findByShortCode(code)
                .orElseThrow(() -> new ShortLinkNotFoundException("Short link not found: " + shortCode));

        if (!shortLink.isEnabled()) {
            throw new ShortLinkNotFoundException("Short link not found: " + shortCode);
        }

        if (shortLink.isExpired(Instant.now())) {
            throw new ShortLinkExpiredException("Short link has expired: " + shortCode);
        }

        // We might want to increment click count here async (Task 7.1), but for now
        // skip it or do sync?
        // The requirement for Phase 4 doesn't explicitly mention click tracking yet,
        // it's in Phase 7.

        return new ShortLinkResponse(
                shortLink.getShortCode().code(),
                shortLink.getOriginalUrl().url(),
                shortLink.getCreatedAt(),
                shortLink.getExpiresAt(),
                shortLink.isEnabled(),
                null // deleteToken not returned when resolving
        );
    }
}
