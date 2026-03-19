package com.tlavu.linkforge.application.usecase;

import com.tlavu.linkforge.domain.entity.ShortLink;
import com.tlavu.linkforge.domain.exception.InvalidDeleteTokenException;
import com.tlavu.linkforge.domain.exception.ShortLinkNotFoundException;
import com.tlavu.linkforge.domain.repository.ShortLinkRepository;
import com.tlavu.linkforge.domain.valueobject.ShortCode;
import com.tlavu.linkforge.infrastructure.cache.ShortLinkCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class DeleteShortLinkUseCaseImpl implements DeleteShortLinkUseCase {

    private final ShortLinkRepository shortLinkRepository;
    private final ShortLinkCacheService shortLinkCacheService;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void execute(String shortCode, String deleteToken) {
        ShortLink shortLink = shortLinkRepository.findByShortCode(ShortCode.of(shortCode))
                .orElseThrow(() -> new ShortLinkNotFoundException("Short link not found: " + shortCode));

        // Secure token validation using matches()
        if (shortLink.getDeleteTokenHash() == null || !passwordEncoder.matches(deleteToken, shortLink.getDeleteTokenHash())) {
            throw new InvalidDeleteTokenException("link.invalid_delete_token");
        }

        shortLinkRepository.delete(shortLink.getId());

        // Evict from cache
        shortLinkCacheService.evictShortLink(shortCode);
    }
}
