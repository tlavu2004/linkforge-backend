package com.tlavu.linkforge.application.usecase;

import com.tlavu.linkforge.application.dto.CreateShortLinkCommand;
import com.tlavu.linkforge.application.dto.ShortLinkResponse;
import com.tlavu.linkforge.domain.entity.ShortLink;
import com.tlavu.linkforge.domain.repository.ShortLinkRepository;
import com.tlavu.linkforge.domain.service.ShortCodeGenerator;
import com.tlavu.linkforge.domain.valueobject.OriginalUrl;
import com.tlavu.linkforge.domain.valueobject.ShortCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateShortLinkUseCaseImpl implements CreateShortLinkUseCase {

    private final ShortLinkRepository shortLinkRepository;
    private final ShortCodeGenerator shortCodeGenerator;

    @Override
    @Transactional
    public ShortLinkResponse execute(CreateShortLinkCommand command) {
        OriginalUrl originalUrl = OriginalUrl.of(command.originalUrl());
        ShortCode shortCode = shortCodeGenerator.generate();

        ShortLink shortLink = ShortLink.create(
                io.hypersistence.tsid.TSID.fast().toLong(),
                shortCode,
                originalUrl,
                command.expiresAt(),
                null // deleteTokenHash - optional for now
        );

        ShortLink savedLink = shortLinkRepository.save(shortLink);

        return new ShortLinkResponse(
                savedLink.getShortCode().code(),
                savedLink.getOriginalUrl().url(),
                savedLink.getCreatedAt(),
                savedLink.getExpiresAt());
    }
}
