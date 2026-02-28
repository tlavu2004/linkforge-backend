package com.tlavu.linkforge.application.usecase;

import io.hypersistence.tsid.TSID;
import com.tlavu.linkforge.application.dto.response.AuthResponse;
import com.tlavu.linkforge.application.dto.request.LoginRequest;
import com.tlavu.linkforge.application.dto.request.RegisterRequest;
import com.tlavu.linkforge.application.dto.response.RegisterResponse;
import com.tlavu.linkforge.application.dto.request.TokenRefreshRequest;
import com.tlavu.linkforge.application.dto.request.LogoutRequest;
import com.tlavu.linkforge.domain.entity.RefreshToken;
import com.tlavu.linkforge.domain.entity.Role;
import com.tlavu.linkforge.domain.entity.User;
import com.tlavu.linkforge.domain.exception.DomainException;
import com.tlavu.linkforge.domain.repository.RefreshTokenRepository;
import com.tlavu.linkforge.domain.repository.UserRepository;
import com.tlavu.linkforge.infrastructure.security.JwtService;
import com.tlavu.linkforge.infrastructure.config.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthUseCase {

        private final UserRepository userRepository;
        private final RefreshTokenRepository refreshTokenRepository;
        private final PasswordEncoder passwordEncoder;
        private final JwtService jwtService;
        private final JwtProperties jwtProperties;
        private final AuthenticationManager authenticationManager;

        public RegisterResponse register(RegisterRequest request) {
                if (userRepository.existsByEmail(request.email())) {
                        throw new DomainException("Email is already taken");
                }

                User user = User.create(
                                TSID.fast().toLong(),
                                request.email(),
                                passwordEncoder.encode(request.password()),
                                Role.USER // Default role is USER
                );

                User savedUser = userRepository.save(user);
                return new RegisterResponse(
                                savedUser.getId(),
                                savedUser.getEmail(),
                                savedUser.getRole(),
                                savedUser.isVipActive(Instant.now()));
        }

        public AuthResponse login(LoginRequest request) {
                authenticationManager.authenticate(
                                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

                User user = userRepository.findByEmail(request.email())
                                .orElseThrow(() -> new DomainException("User not found"));

                return generateAuthResponse(user);
        }

        public AuthResponse refreshToken(TokenRefreshRequest request) {
                RefreshToken refreshTokenEntity = refreshTokenRepository.findByToken(request.refreshToken())
                                .orElseThrow(() -> new DomainException("Invalid refresh token"));

                if (refreshTokenEntity.isExpired(Instant.now())) {
                        refreshTokenRepository.deleteByToken(refreshTokenEntity.getToken());
                        throw new DomainException("Refresh token was expired. Please make a new signin request");
                }

                User user = userRepository.findById(refreshTokenEntity.getUserId())
                                .orElseThrow(() -> new DomainException("User not found"));

                // Issue new access token
                String token = jwtService.generateToken(user.getEmail(), user.getId(), user.getRole().name());

                return new AuthResponse(
                                token,
                                refreshTokenEntity.getToken(),
                                user.getId(),
                                user.getEmail(),
                                user.getRole(),
                                user.isVipActive(Instant.now()));
        }

        public void logout(LogoutRequest request) {
                refreshTokenRepository.deleteByToken(request.refreshToken());
        }

        private AuthResponse generateAuthResponse(User user) {
                String token = jwtService.generateToken(user.getEmail(), user.getId(), user.getRole().name());
                RefreshToken refreshToken = createRefreshToken(user.getId());

                return new AuthResponse(
                                token,
                                refreshToken.getToken(),
                                user.getId(),
                                user.getEmail(),
                                user.getRole(),
                                user.isVipActive(Instant.now()));
        }

        private RefreshToken createRefreshToken(Long userId) {
                RefreshToken refreshToken = new RefreshToken(
                                TSID.fast().toLong(),
                                userId,
                                UUID.randomUUID().toString(),
                                Instant.now().plusMillis(jwtProperties.getRefreshTokenExpiration()));
                return refreshTokenRepository.save(refreshToken);
        }
}
