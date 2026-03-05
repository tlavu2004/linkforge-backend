package com.tlavu.linkforge.application.usecase;

import com.tlavu.linkforge.application.dto.response.ShortLinkResponse;
import com.tlavu.linkforge.domain.entity.Role;
import com.tlavu.linkforge.domain.entity.ShortLink;
import com.tlavu.linkforge.domain.entity.User;
import com.tlavu.linkforge.domain.exception.DomainException;
import com.tlavu.linkforge.domain.repository.ShortLinkRepository;
import com.tlavu.linkforge.domain.repository.UserRepository;
import com.tlavu.linkforge.domain.valueobject.ShortCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class DeleteQrCodeUseCaseImpl implements DeleteQrCodeUseCase {

    private final ShortLinkRepository shortLinkRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public ShortLinkResponse execute(String shortCode, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new DomainException("User not found"));

        ShortLink shortLink = shortLinkRepository.findByShortCode(ShortCode.of(shortCode))
                .orElseThrow(() -> new DomainException("Short link not found"));

        // Check ownership - users can delete their own QR codes even if VIP expired
        if (!shortLink.getUserId().equals(userId)) {
            throw new DomainException("You do not own this link");
        }

        shortLink.deleteQrCode();
        ShortLink saved = shortLinkRepository.save(shortLink);

        boolean isVipActive = user.isVipActive(Instant.now());
        boolean isAdmin = user.getRole() == Role.ADMIN;

        return toResponse(saved, isVipActive || isAdmin);
    }

    private ShortLinkResponse toResponse(ShortLink shortLink, boolean skipAds) {
        return new ShortLinkResponse(
                shortLink.getShortCode().code(),
                shortLink.getOriginalUrl().url(),
                shortLink.getCreatedAt(),
                shortLink.getExpiresAt(),
                shortLink.getDeleteTokenHash(),
                skipAds,
                shortLink.getQrCode());
    }
}
