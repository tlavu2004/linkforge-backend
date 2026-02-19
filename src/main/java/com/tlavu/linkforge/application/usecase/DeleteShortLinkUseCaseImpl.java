package com.tlavu.linkforge.application.usecase;

import com.tlavu.linkforge.domain.entity.ShortLink;
import com.tlavu.linkforge.domain.exception.InvalidDeleteTokenException;
import com.tlavu.linkforge.domain.exception.ShortLinkNotFoundException;
import com.tlavu.linkforge.domain.repository.ShortLinkRepository;
import com.tlavu.linkforge.domain.valueobject.ShortCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeleteShortLinkUseCaseImpl implements DeleteShortLinkUseCase {

    private final ShortLinkRepository shortLinkRepository;

    @Override
    @Transactional
    public void execute(String shortCode, String deleteToken) {
        ShortLink shortLink = shortLinkRepository.findByShortCode(ShortCode.of(shortCode))
                .orElseThrow(() -> new ShortLinkNotFoundException("Short link not found: " + shortCode));

        // Basic token validation (equality check)
        // In real app, we might hash input token and compare with stored hash
        // Here we assume simple string comparison for MVP as decided
        if (shortLink.getDeleteTokenHash() == null || !shortLink.getDeleteTokenHash().equals(deleteToken)) {
            throw new InvalidDeleteTokenException("Invalid delete token");
        }

        shortLink.disable();
        shortLinkRepository.save(shortLink);
    }
}
