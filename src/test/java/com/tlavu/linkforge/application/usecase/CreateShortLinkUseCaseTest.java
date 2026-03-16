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
import org.springframework.security.crypto.password.PasswordEncoder;
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

    @Mock
    private PasswordEncoder passwordEncoder;

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
        CreateShortLinkCommand command = new CreateShortLinkCommand(originalUrl, null, null); // No custom expiration, no alias
        ShortCode expectedCode = ShortCode.of("abc12345");

        when(passwordEncoder.encode(anyString())).thenReturn("hashed-token");
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
        CreateShortLinkCommand command = new CreateShortLinkCommand(originalUrl, expiresAt, null);

        // When/Then
        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(DomainException.class)
                .hasMessage("Only VIP users can set custom expiration time for short links");

        verify(shortLinkRepository, never()).save(any(ShortLink.class));
        verify(metricsService, never()).incrementLinksCreated();
    }

    @Test
    @DisplayName("Should create short link with valid custom alias")
    void shouldCreateShortLinkWithValidCustomAlias() {
        // Given
        String originalUrl = "http://example.com";
        String customAlias = "my-custom-link";
        CreateShortLinkCommand command = new CreateShortLinkCommand(originalUrl, null, customAlias);
        ShortCode expectedCode = ShortCode.of(customAlias);

        when(passwordEncoder.encode(anyString())).thenReturn("hashed-token");
        when(shortLinkRepository.existsByShortCode(expectedCode)).thenReturn(false);
        when(shortLinkRepository.save(any(ShortLink.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ShortLinkResponse response = useCase.execute(command);

        // Then
        assertThat(response.shortCode()).isEqualTo(customAlias);
        verify(shortLinkRepository).existsByShortCode(expectedCode);
        verify(shortCodeGenerator, never()).generate();
        verify(shortLinkRepository).save(any(ShortLink.class));
    }

    @Test
    @DisplayName("Should throw exception if custom alias is already taken")
    void shouldThrowExceptionIfAliasTaken() {
        // Given
        String originalUrl = "http://example.com";
        String customAlias = "taken-alias";
        CreateShortLinkCommand command = new CreateShortLinkCommand(originalUrl, null, customAlias);
        ShortCode expectedCode = ShortCode.of(customAlias);

        when(shortLinkRepository.existsByShortCode(expectedCode)).thenReturn(true);

        // When/Then
        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("already taken");

        verify(shortLinkRepository, never()).save(any(ShortLink.class));
    }

    @Test
    @DisplayName("Should throw exception if custom alias is a reserved word")
    void shouldThrowExceptionIfAliasReserved() {
        // Given
        String originalUrl = "http://example.com";
        String customAlias = "admin";
        CreateShortLinkCommand command = new CreateShortLinkCommand(originalUrl, null, customAlias);

        // When/Then
        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("reserved word");

        verify(shortLinkRepository, never()).save(any(ShortLink.class));
    }

    @Test
    @DisplayName("Should throw exception if custom alias has invalid format")
    void shouldThrowExceptionIfAliasInvalidFormat() {
        // Given
        String originalUrl = "http://example.com";
        String customAlias = "invalid @ alias";
        CreateShortLinkCommand command = new CreateShortLinkCommand(originalUrl, null, customAlias);

        // When/Then
        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("only contain letters, numbers, hyphens, and underscores");

        verify(shortLinkRepository, never()).save(any(ShortLink.class));
    }
}
