package com.tlavu.linkforge.application.usecase;

import com.tlavu.linkforge.application.dto.response.LinkStatsResponse;
import com.tlavu.linkforge.domain.entity.ShortLink;
import com.tlavu.linkforge.domain.entity.User;
import com.tlavu.linkforge.domain.repository.ClickAnalyticsRepository;
import com.tlavu.linkforge.domain.repository.ShortLinkRepository;
import com.tlavu.linkforge.domain.valueobject.OriginalUrl;
import com.tlavu.linkforge.domain.valueobject.ShortCode;
import com.tlavu.linkforge.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class GetLinkAnalyticsUseCaseTest {

    @Mock
    private ShortLinkRepository shortLinkRepository;

    @Mock
    private ClickAnalyticsRepository clickAnalyticsRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private GetLinkAnalyticsUseCaseImpl useCase;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    @DisplayName("Should return stats when user is owner")
    void shouldReturnStatsWhenUserIsOwner() {
        // Given
        String shortCode = "abc12345";
        String email = "test@example.com";
        Long userId = 100L;

        User user = mock(User.class);
        when(user.getId()).thenReturn(userId);

        ShortLink link = new ShortLink(1L, ShortCode.of(shortCode), OriginalUrl.of("http://example.com"),
                Instant.now(), null, 0L, userId, "hash", null);

        when(shortLinkRepository.findByShortCode(ShortCode.of(shortCode))).thenReturn(Optional.of(link));
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(email);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        when(clickAnalyticsRepository.countTotalClicks(shortCode)).thenReturn(100L);
        when(clickAnalyticsRepository.countUniqueVisitors(shortCode)).thenReturn(50L);
        when(clickAnalyticsRepository.countByCountry(shortCode)).thenReturn(Collections.emptyMap());
        when(clickAnalyticsRepository.countByDeviceType(eq(shortCode))).thenReturn(Collections.emptyMap());
        when(clickAnalyticsRepository.countByReferrer(eq(shortCode))).thenReturn(Collections.emptyMap());
        when(clickAnalyticsRepository.getDailyClickStats(eq(shortCode), any(), any()))
                .thenReturn(Collections.emptyMap());

        // When
        LinkStatsResponse response = useCase.execute(shortCode, Instant.now().minusSeconds(3600), Instant.now());

        // Then
        assertThat(response).isNotNull();
        assertThat(response.shortCode()).isEqualTo(shortCode);
        assertThat(response.totalClicks()).isEqualTo(100);
    }

    @Test
    @DisplayName("Should throw AccessDeniedException when user is not owner")
    void shouldThrowAccessDeniedException() {
        // Given
        String shortCode = "abc12345";
        String email = "other@example.com";
        Long ownerId = 100L;
        Long otherId = 200L;

        User otherUser = mock(User.class);
        when(otherUser.getId()).thenReturn(otherId);

        ShortLink link = new ShortLink(1L, ShortCode.of(shortCode), OriginalUrl.of("http://example.com"),
                Instant.now(), null, 0L, ownerId, "hash", null);

        when(shortLinkRepository.findByShortCode(ShortCode.of(shortCode))).thenReturn(Optional.of(link));
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(email);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(otherUser));

        // When/Then
        assertThatThrownBy(() -> useCase.execute(shortCode, Instant.now(), Instant.now()))
                .isInstanceOf(AccessDeniedException.class);
    }
}
