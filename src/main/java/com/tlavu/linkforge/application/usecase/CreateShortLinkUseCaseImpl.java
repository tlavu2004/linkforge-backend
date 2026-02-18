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

        // Check for collision?
        // With TSID, collision probability is extremely low.
        // But for safety, we could check. However, for MVP/Phase 1 we might trust TSID
        // uniqueness.
        // Let's implement basic flow first.

        ShortLink shortLink = ShortLink.create(
                // Wait, ShortLink.create checks "ID cannot be null".
                // Ah, the ID in ShortLink entity is the DB ID (Long).
                // When creating NEW entity, we usually don't have ID yet if using IDENTITY
                // strategy.
                // Let's check ShortLink.java again.
                // Re-checking ShortLink.create:
                // if (id == null) { throw new InvalidShortLinkException("ID cannot be null"); }
                // This seems too strict for creation if ID is auto-generated.
                // But TSID can also be the ID?
                // The current ShortLinkJpaEntity uses @Id private Long id;
                // And V1 sql says: id BIGINT PRIMARY KEY.
                // It does NOT say GENERATED ALWAYS AS IDENTITY.
                // So we probably need to provide ID.
                // Strategy: Generate TSID as ID as well? Or use the same Long for ID and
                // ShortCode encoding?
                // The TsidShortCodeGenerator generates a TSID long, then encodes it.
                // We can reuse that long as the ID!
                // BUT TsidShortCodeGenerator only returns ShortCode (string).
                // We might need to refactor or just generate another ID.
                // Generating 2 TSIDs is fine.
                // Let's generate a TSID for the ID.

                // WAIT. If I use TSID for ShortCode, I can decode it to get back the ID?
                // Base62Encoder.encode(long).
                // If we want [ID] -> [Base62] -> [ShortCode], then ID and ShortCode are 1-to-1.
                // So we should probably use the SAME value.
                // But ShortCodeGenerator defines `ShortCode generate()`. It hides the long
                // value.
                // If we want to use that long as PK, we need access to it.
                // OR we just generate a separate ID for PK.
                // Let's generate a separate ID for PK using TSID as well (common practice).
                // I'll add a TsidFactory or just use TSID.fast().toLong() here if library is
                // available.
                // But wait, do I have TSID library in this class?
                // `io.hypersistence.tsid.TSID` is a dependency.
                // It is better if we have a IdGenerator interface, but for now direct usage or
                // separate ID is okay.
                // Let's assume separate ID for now to decouple PK from ShortCode (even though
                // they could be same).

                // ShortLink.create(Long id, ...).
                // I need to provide an ID.
                // I'll use TSID.fast().toLong() for the ID as well.

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
