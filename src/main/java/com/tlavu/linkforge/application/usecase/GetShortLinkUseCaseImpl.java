package com.tlavu.linkforge.application.usecase;

import com.tlavu.linkforge.application.dto.response.ShortLinkResponse;
import com.tlavu.linkforge.domain.entity.ShortLink;
import com.tlavu.linkforge.domain.exception.ShortLinkNotFoundException;
import com.tlavu.linkforge.domain.repository.ShortLinkRepository;
import com.tlavu.linkforge.domain.valueobject.ShortCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetShortLinkUseCaseImpl implements GetShortLinkUseCase {

    private final ShortLinkRepository shortLinkRepository;

    @Override
    @Transactional(readOnly = true)
    public ShortLinkResponse execute(String shortCode) {
        ShortLink shortLink = shortLinkRepository.findByShortCode(ShortCode.of(shortCode))
                .orElseThrow(() -> new ShortLinkNotFoundException("Short link not found: " + shortCode));

        return new ShortLinkResponse(
                shortLink.getShortCode().code(),
                shortLink.getOriginalUrl().url(),
                shortLink.getCreatedAt(),
                shortLink.getExpiresAt(),
                null, // Do not return deleteToken
                false, // skipAds is irrelevant for Get API
                shortLink.getQrCode());
    }
}
