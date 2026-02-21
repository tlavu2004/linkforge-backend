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
    private final com.tlavu.linkforge.infrastructure.metrics.MetricsService metricsService;

    @Override
    @Transactional
    public ShortLinkResponse execute(CreateShortLinkCommand command) {
        OriginalUrl originalUrl = OriginalUrl.of(command.originalUrl());
        ShortCode shortCode = shortCodeGenerator.generate();
        String deleteToken = java.util.UUID.randomUUID().toString();

        ShortLink shortLink = ShortLink.create(
                io.hypersistence.tsid.TSID.fast().toLong(),
                shortCode,
                originalUrl,
                command.expiresAt(),
                deleteToken);

        ShortLink savedLink = shortLinkRepository.save(shortLink);

        metricsService.incrementLinksCreated();

        return new ShortLinkResponse(
                savedLink.getShortCode().code(),
                savedLink.getOriginalUrl().url(),
                savedLink.getCreatedAt(),
                savedLink.getExpiresAt(),
                savedLink.isEnabled(),
                savedLink.getDeleteTokenHash());
    }
}
