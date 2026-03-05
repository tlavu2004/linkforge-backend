package com.tlavu.linkforge.application.usecase;

import com.tlavu.linkforge.application.dto.response.ShortLinkResponse;
import com.tlavu.linkforge.domain.entity.Role;
import com.tlavu.linkforge.domain.entity.ShortLink;
import com.tlavu.linkforge.domain.entity.User;
import com.tlavu.linkforge.domain.exception.DomainException;
import com.tlavu.linkforge.domain.repository.ShortLinkRepository;
import com.tlavu.linkforge.domain.repository.UserRepository;
import com.tlavu.linkforge.domain.valueobject.ShortCode;
import com.tlavu.linkforge.infrastructure.util.QrCodeGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class GenerateQrCodeUseCaseImpl implements GenerateQrCodeUseCase {

    private final ShortLinkRepository shortLinkRepository;
    private final UserRepository userRepository;
    private final QrCodeGenerator qrCodeGenerator;

    @Value("${application.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @Override
    @Transactional
    public ShortLinkResponse execute(String shortCode, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new DomainException("User not found"));

        ShortLink shortLink = shortLinkRepository.findByShortCode(ShortCode.of(shortCode))
                .orElseThrow(() -> new DomainException("Short link not found"));

        // Check ownership
        if (!shortLink.getUserId().equals(userId)) {
            throw new DomainException("You do not own this link");
        }

        // Check VIP/Admin status for GENERATION
        boolean isVipActive = user.isVipActive(Instant.now());
        boolean isAdmin = user.getRole() == Role.ADMIN;

        if (!isAdmin && !isVipActive) {
            throw new DomainException("Only VIP or Admin users can generate QR codes");
        }

        if (shortLink.getQrCode() != null) {
            return toResponse(shortLink, isVipActive || isAdmin);
        }

        String targetUrl = frontendUrl + "/r/" + shortLink.getShortCode().code();
        String qrCodeBase64 = qrCodeGenerator.generateQrCodeBase64(targetUrl, 300, 300);

        shortLink.assignQrCode(qrCodeBase64);
        ShortLink saved = shortLinkRepository.save(shortLink);

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
