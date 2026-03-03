package com.tlavu.linkforge.application.usecase;

import com.tlavu.linkforge.application.dto.command.CreateShortLinkCommand;
import com.tlavu.linkforge.application.dto.response.ShortLinkResponse;
import com.tlavu.linkforge.domain.entity.ShortLink;
import com.tlavu.linkforge.domain.exception.DomainException;
import com.tlavu.linkforge.domain.repository.ShortLinkRepository;
import com.tlavu.linkforge.domain.repository.UserRepository;
import com.tlavu.linkforge.domain.service.ShortCodeGenerator;
import com.tlavu.linkforge.domain.valueobject.ShortCode;
import com.tlavu.linkforge.infrastructure.metrics.MetricsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateShortLinkUseCaseTest {

    @Mock
    private ShortLinkRepository shortLinkRepository;

    @Mock
    private ShortCodeGenerator shortCodeGenerator;

    @Mock
    private MetricsService metricsService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private jakarta.servlet.http.HttpServletRequest request;

    @Mock
    private com.tlavu.linkforge.infrastructure.security.JwtService jwtService;

    @InjectMocks
    private CreateShortLinkUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should create short link with default 30-day expiration for anonymous user")
    void shouldCreateShortLinkWithDefault30DayExpiration() {
        // Given
        String originalUrl = "http://example.com";
        CreateShortLinkCommand command = new CreateShortLinkCommand(originalUrl, null); // No custom expiration
        ShortCode expectedCode = ShortCode.of("abc12345");

        when(shortCodeGenerator.generate()).thenReturn(expectedCode);
        when(shortLinkRepository.save(any(ShortLink.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Instant beforeExecution = Instant.now();
        ShortLinkResponse response = useCase.execute(command);
        Instant afterExecution = Instant.now();

        // Then
        assertThat(response).isNotNull();
        assertThat(response.shortCode()).isEqualTo(expectedCode.code());
        assertThat(response.originalUrl()).isEqualTo(originalUrl);
        assertThat(response.createdAt()).isNotNull();
        // Anonymous user should get default 30-day expiration
        assertThat(response.expiresAt()).isNotNull();
        assertThat(response.expiresAt()).isBetween(
                beforeExecution.plus(30, ChronoUnit.DAYS),
                afterExecution.plus(30, ChronoUnit.DAYS));

        verify(shortCodeGenerator).generate();
        verify(shortLinkRepository).save(any(ShortLink.class));
        verify(metricsService).incrementLinksCreated();
    }

    @Test
    @DisplayName("Should throw exception if non-VIP user attempts to set expiration time")
    void shouldThrowExceptionIfNonVipSetsExpiration() {
        // Given
        String originalUrl = "http://example.com";
        Instant expiresAt = Instant.now().plusSeconds(3600);
        CreateShortLinkCommand command = new CreateShortLinkCommand(originalUrl, expiresAt);

        // When/Then
        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(DomainException.class)
                .hasMessage("Only VIP users can set custom expiration time for short links");

        verify(shortLinkRepository, never()).save(any(ShortLink.class));
        verify(metricsService, never()).incrementLinksCreated();
    }
}
