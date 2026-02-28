package com.tlavu.linkforge.application.usecase;

import com.tlavu.linkforge.application.dto.response.AuthResponse;
import com.tlavu.linkforge.application.dto.request.LoginRequest;
import com.tlavu.linkforge.application.dto.request.RegisterRequest;
import com.tlavu.linkforge.application.dto.response.RegisterResponse;
import com.tlavu.linkforge.domain.entity.Role;
import com.tlavu.linkforge.domain.entity.User;
import com.tlavu.linkforge.domain.exception.DomainException;
import com.tlavu.linkforge.domain.repository.UserRepository;
import com.tlavu.linkforge.infrastructure.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.tlavu.linkforge.infrastructure.config.JwtProperties;
import com.tlavu.linkforge.domain.entity.RefreshToken;
import com.tlavu.linkforge.domain.repository.RefreshTokenRepository;

import java.time.Instant;

@ExtendWith(MockitoExtension.class)
class AuthUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private JwtProperties jwtProperties;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthUseCase authUseCase;

    private User mockUser;
    private RefreshToken mockRefreshToken;

    @BeforeEach
    void setUp() {
        mockUser = User.create(1L, "test@example.com", "hashed_password", Role.USER);
        mockRefreshToken = new RefreshToken(1L, 1L, "mock_refresh_token", Instant.now().plusMillis(604800000L));
    }

    @Test
    @DisplayName("Should successfully register a new user")
    void shouldRegisterUser() {
        // Arrange
        RegisterRequest request = new RegisterRequest("test@example.com", "password123");
        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("hashed_password");
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        // Act
        RegisterResponse response = authUseCase.register(request);

        // Assert
        assertThat(response.email()).isEqualTo("test@example.com");
        assertThat(response.role()).isEqualTo(Role.USER);
        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.vip()).isFalse();

        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception if email is already taken during registration")
    void shouldThrowExceptionIfEmailTaken() {
        // Arrange
        RegisterRequest request = new RegisterRequest("taken@example.com", "password123");
        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> authUseCase.register(request))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("taken");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should successfully login a user")
    void shouldLoginUser() {
        // Arrange
        LoginRequest request = new LoginRequest("test@example.com", "password123");
        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(mockUser));
        when(jwtService.generateToken(anyString(), anyLong(), anyString())).thenReturn("mock_jwt_token");
        when(jwtProperties.getRefreshTokenExpiration()).thenReturn(604800000L);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(mockRefreshToken);

        // Act
        AuthResponse response = authUseCase.login(request);

        // Assert
        assertThat(response.accessToken()).isEqualTo("mock_jwt_token");
        assertThat(response.email()).isEqualTo(mockUser.getEmail());
        assertThat(response.role()).isEqualTo(mockUser.getRole());

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    @DisplayName("Should throw exception if user not found during login")
    void shouldThrowExceptionIfUserNotFound() {
        // Arrange
        LoginRequest request = new LoginRequest("notfound@example.com", "password123");
        when(userRepository.findByEmail(request.email())).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> authUseCase.login(request))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("not found");
    }
}
