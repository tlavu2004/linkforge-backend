package com.tlavu.linkforge.application.usecase;

import com.tlavu.linkforge.application.dto.response.ShortLinkResponse;
import com.tlavu.linkforge.domain.entity.ShortLink;
import com.tlavu.linkforge.domain.exception.ShortLinkExpiredException;
import com.tlavu.linkforge.domain.exception.ShortLinkNotFoundException;
import com.tlavu.linkforge.domain.repository.ShortLinkRepository;
import com.tlavu.linkforge.domain.valueobject.OriginalUrl;
import com.tlavu.linkforge.domain.valueobject.ShortCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResolveShortLinkUseCaseTest {

    @Mock
    private ShortLinkRepository shortLinkRepository;

    @Mock
    private com.tlavu.linkforge.infrastructure.cache.ShortLinkCacheService shortLinkCacheService;

    @Mock
    private com.tlavu.linkforge.infrastructure.metrics.MetricsService metricsService;

    @Mock
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ResolveShortLinkUseCaseImpl useCase;

    @Test
    @DisplayName("Should resolve short link successfully (Cache Miss)")
    void shouldResolveShortLinkSuccessfully_CacheMiss() {
        // Given
        String codeStr = "abc12345";
        ShortCode code = ShortCode.of(codeStr);
        ShortLink shortLink = ShortLink.create(1L, code, OriginalUrl.of("http://example.com"), null, null, "hash");

        when(shortLinkCacheService.getShortLink(codeStr)).thenReturn(Optional.empty());
        when(shortLinkRepository.findByShortCode(any(ShortCode.class))).thenReturn(Optional.of(shortLink));

        // When
        ShortLinkResponse response = useCase.execute(codeStr);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.shortCode()).isEqualTo(codeStr);
        assertThat(response.originalUrl()).isEqualTo("http://example.com");
        assertThat(response.deleteToken()).isNull();

        verify(shortLinkCacheService).getShortLink(codeStr);
        verify(shortLinkRepository).findByShortCode(any(ShortCode.class));
        verify(shortLinkCacheService).saveShortLink(eq(codeStr), any(ShortLinkResponse.class));
    }

    @Test
    @DisplayName("Should resolve short link from Cache (Cache Hit)")
    void shouldResolveShortLinkFromCache_CacheHit() {
        // Given
        String codeStr = "abc12345";
        ShortLinkResponse cachedResponse = new ShortLinkResponse(codeStr, "http://example.com", Instant.now(), null,
                null, false);

        when(shortLinkCacheService.getShortLink(codeStr)).thenReturn(Optional.of(cachedResponse));

        // When
        ShortLinkResponse response = useCase.execute(codeStr);

        // Then
        assertThat(response).isEqualTo(cachedResponse);
        verify(shortLinkCacheService).getShortLink(codeStr);
        verify(shortLinkRepository, org.mockito.Mockito.never()).findByShortCode(any());
    }

    @Test
    @DisplayName("Should throw NotFoundException when link does not exist")
    void shouldThrowNotFoundException() {
        // Given
        String codeStr = "notfound";
        when(shortLinkCacheService.getShortLink(codeStr)).thenReturn(Optional.empty());
        when(shortLinkRepository.findByShortCode(any(ShortCode.class))).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> useCase.execute(codeStr))
                .isInstanceOf(ShortLinkNotFoundException.class)
                .hasMessageContaining(codeStr);
    }

    @Test
    @DisplayName("Should throw ExpiredException when link is expired")
    void shouldThrowExpiredException() {
        // Given
        String codeStr = "expired";
        ShortCode code = ShortCode.of(codeStr);
        Instant past = Instant.now().minus(1, ChronoUnit.DAYS);
        // We have to bypass validation in ShortLink.create which checks expiration >
        // now.
        // We can use the reconstruction constructor (public constructor).
        ShortLink expiredLink = new ShortLink(1L, code, OriginalUrl.of("http://example.com"),
                Instant.now().minus(2, ChronoUnit.DAYS), past, 0L, null, "hash");

        when(shortLinkCacheService.getShortLink(codeStr)).thenReturn(Optional.empty());
        when(shortLinkRepository.findByShortCode(any(ShortCode.class))).thenReturn(Optional.of(expiredLink));

        // When/Then
        assertThatThrownBy(() -> useCase.execute(codeStr))
                .isInstanceOf(ShortLinkExpiredException.class)
                .hasMessageContaining(codeStr);
    }

}
