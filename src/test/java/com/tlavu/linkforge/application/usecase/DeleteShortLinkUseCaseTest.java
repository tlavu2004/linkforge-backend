package com.tlavu.linkforge.application.usecase;

import com.tlavu.linkforge.domain.entity.ShortLink;
import com.tlavu.linkforge.domain.exception.InvalidDeleteTokenException;
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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeleteShortLinkUseCaseTest {

    @Mock
    private ShortLinkRepository shortLinkRepository;

    @Mock
    private com.tlavu.linkforge.infrastructure.cache.ShortLinkCacheService shortLinkCacheService;

    @InjectMocks
    private DeleteShortLinkUseCaseImpl deleteShortLinkUseCase;

    @Test
    @DisplayName("Should disable short link when token is valid")
    void shouldDisableShortLinkWhenTokenIsValid() {
        // Given
        String shortCodeStr = "abc12345";
        String deleteToken = "valid-token";
        ShortCode shortCode = ShortCode.of(shortCodeStr);
        ShortLink shortLink = ShortLink.create(
                1L,
                shortCode,
                OriginalUrl.of("http://example.com"),
                null,
                deleteToken);

        when(shortLinkRepository.findByShortCode(shortCode)).thenReturn(Optional.of(shortLink));

        // When
        deleteShortLinkUseCase.execute(shortCodeStr, deleteToken);

        // Then
        assertFalse(shortLink.isEnabled());
        verify(shortLinkRepository).save(shortLink);
        verify(shortLinkCacheService).evictShortLink(shortCodeStr);
    }

    @Test
    @DisplayName("Should throw exception when token is invalid")
    void shouldThrowExceptionWhenTokenIsInvalid() {
        // Given
        String shortCodeStr = "abc12345";
        String validToken = "valid-token";
        String invalidToken = "invalid-token";
        ShortLink shortLink = ShortLink.create(
                1L,
                ShortCode.of(shortCodeStr),
                OriginalUrl.of("http://example.com"),
                null,
                validToken);

        when(shortLinkRepository.findByShortCode(ShortCode.of(shortCodeStr))).thenReturn(Optional.of(shortLink));

        // When/Then
        assertThrows(InvalidDeleteTokenException.class,
                () -> deleteShortLinkUseCase.execute(shortCodeStr, invalidToken));
        verify(shortLinkRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when link not found")
    void shouldThrowExceptionWhenLinkNotFound() {
        // Given
        String shortCodeStr = "notfound";
        when(shortLinkRepository.findByShortCode(ShortCode.of(shortCodeStr))).thenReturn(Optional.empty());

        // When/Then
        assertThrows(ShortLinkNotFoundException.class, () -> deleteShortLinkUseCase.execute(shortCodeStr, "token"));
    }
}
