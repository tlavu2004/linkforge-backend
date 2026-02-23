package com.tlavu.linkforge.application.usecase;

import com.tlavu.linkforge.domain.entity.Role;
import com.tlavu.linkforge.domain.entity.User;
import com.tlavu.linkforge.domain.exception.DomainException;
import com.tlavu.linkforge.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ToggleVipStatusUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ToggleVipStatusUseCase toggleVipStatusUseCase;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = User.create(1L, "admin@example.com", "hash", Role.ADMIN);
    }

    @Test
    @DisplayName("Should successfully grant lifetime VIP")
    void shouldGrantLifetimeVip() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));

        // Act
        toggleVipStatusUseCase.execute(1L, true);

        // Assert
        assertThat(mockUser.isVip()).isTrue();
        assertThat(mockUser.getVipExpiresAt()).isNull();
        verify(userRepository).save(mockUser);
    }

    @Test
    @DisplayName("Should successfully revoke VIP")
    void shouldRevokeVip() {
        // Arrange
        mockUser.grantLifetimeVip(); // Start as VIP
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));

        // Act
        toggleVipStatusUseCase.execute(1L, false);

        // Assert
        assertThat(mockUser.isVip()).isFalse();
        verify(userRepository).save(mockUser);
    }

    @Test
    @DisplayName("Should throw exception if user not found")
    void shouldThrowExceptionIfUserNotFound() {
        // Arrange
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> toggleVipStatusUseCase.execute(99L, true))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("not found");
    }
}
