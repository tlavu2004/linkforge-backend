package com.tlavu.linkforge.application.usecase;

import com.tlavu.linkforge.application.dto.response.ShortLinkResponse;
import com.tlavu.linkforge.domain.entity.ShortLink;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetShortLinkUseCaseTest {

    @Mock
    private ShortLinkRepository shortLinkRepository;

    @InjectMocks
    private GetShortLinkUseCaseImpl getShortLinkUseCase;

    @Test
    @DisplayName("Should return short link info when found")
    void shouldReturnShortLinkInfo() {
        // Given
        String shortCodeStr = "abc12345";
        ShortCode shortCode = ShortCode.of(shortCodeStr);
        ShortLink shortLink = ShortLink.create(
                1L,
                shortCode,
                OriginalUrl.of("http://example.com"),
                Instant.now().plusSeconds(3600),
                null,
                "delete-token");

        when(shortLinkRepository.findByShortCode(shortCode)).thenReturn(Optional.of(shortLink));

        // When
        ShortLinkResponse response = getShortLinkUseCase.execute(shortCodeStr);

        // Then
        assertThat(response.shortCode()).isEqualTo(shortCodeStr);
        assertThat(response.originalUrl()).isEqualTo("http://example.com");
        assertThat(response.deleteToken()).isNull(); // Token should not be returned
    }

    @Test
    @DisplayName("Should throw exception when link not found")
    void shouldThrowExceptionWhenNotFound() {
        // Given
        String shortCodeStr = "notfound";
        when(shortLinkRepository.findByShortCode(ShortCode.of(shortCodeStr))).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> getShortLinkUseCase.execute(shortCodeStr))
                .isInstanceOf(ShortLinkNotFoundException.class);
    }
}
