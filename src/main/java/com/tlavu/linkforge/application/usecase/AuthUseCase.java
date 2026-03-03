package com.tlavu.linkforge.application.usecase;

import io.hypersistence.tsid.TSID;
import com.tlavu.linkforge.application.dto.response.AuthResponse;
import com.tlavu.linkforge.application.dto.request.LoginRequest;
import com.tlavu.linkforge.application.dto.request.RegisterRequest;
import com.tlavu.linkforge.application.dto.request.ForgotPasswordRequest;
import com.tlavu.linkforge.application.dto.request.ResetPasswordRequest;
import com.tlavu.linkforge.application.dto.request.VerifyEmailRequest;
import com.tlavu.linkforge.application.dto.request.ResendOtpRequest;
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
import com.tlavu.linkforge.infrastructure.service.OtpService;
import com.tlavu.linkforge.infrastructure.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class AuthUseCase {

        private final UserRepository userRepository;
        private final RefreshTokenRepository refreshTokenRepository;
        private final PasswordEncoder passwordEncoder;
        private final JwtService jwtService;
        private final JwtProperties jwtProperties;
        private final AuthenticationManager authenticationManager;
        private final OtpService otpService;
        private final EmailService emailService;

        public RegisterResponse register(RegisterRequest request) {
                if (userRepository.existsByEmail(request.email())) {
                        throw new DomainException("Email is already taken");
                }

                User user = User.create(
                                TSID.fast().toLong(),
                                request.name(),
                                request.email(),
                                passwordEncoder.encode(request.password()),
                                Role.USER);

                User savedUser = userRepository.save(user);

                // Generate and send verification OTP
                String otp = otpService.generateAndStore(savedUser.getEmail(), "verify-email");
                emailService.sendVerificationEmail(savedUser.getEmail(), otp);

                return new RegisterResponse(
                                savedUser.getId(),
                                savedUser.getName(),
                                savedUser.getEmail(),
                                savedUser.getRole(),
                                savedUser.isVipActive(Instant.now()));
        }

        public AuthResponse login(LoginRequest request) {
                authenticationManager.authenticate(
                                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

                User user = userRepository.findByEmail(request.email())
                                .orElseThrow(() -> new DomainException("User not found"));

                if (!user.isEmailVerified()) {
                        throw new DomainException(
                                        "Email not verified. Please check your email for the verification code.");
                }

                return generateAuthResponse(user);
        }

        public void verifyEmail(VerifyEmailRequest request) {
                User user = userRepository.findByEmail(request.email())
                                .orElseThrow(() -> new DomainException("User not found"));

                if (user.isEmailVerified()) {
                        throw new DomainException("Email is already verified");
                }

                if (!otpService.verify(request.email(), "verify-email", request.otp())) {
                        throw new DomainException("Invalid or expired OTP");
                }

                user.verifyEmail();
                userRepository.save(user);
        }

        public void resendVerificationOtp(ResendOtpRequest request) {
                User user = userRepository.findByEmail(request.email())
                                .orElseThrow(() -> new DomainException("User not found"));

                if (user.isEmailVerified()) {
                        throw new DomainException("Email is already verified");
                }

                String otp = otpService.generateAndStore(user.getEmail(), "verify-email");
                emailService.sendVerificationEmail(user.getEmail(), otp);
        }

        public void forgotPassword(ForgotPasswordRequest request) {
                User user = userRepository.findByEmail(request.email())
                                .orElseThrow(() -> new DomainException("User not found"));

                String otp = otpService.generateAndStore(user.getEmail(), "reset-password");
                emailService.sendPasswordResetEmail(user.getEmail(), otp);
        }

        public void resetPassword(ResetPasswordRequest request) {
                User user = userRepository.findByEmail(request.email())
                                .orElseThrow(() -> new DomainException("User not found"));

                if (!otpService.verify(request.email(), "reset-password", request.otp())) {
                        throw new DomainException("Invalid or expired OTP");
                }

                user.updatePassword(passwordEncoder.encode(request.newPassword()));
                userRepository.save(user);
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

                String token = jwtService.generateToken(user.getEmail(), user.getId(), user.getRole().name());

                return new AuthResponse(
                                token,
                                refreshTokenEntity.getToken(),
                                user.getId(),
                                user.getName(),
                                user.getEmail(),
                                user.getRole(),
                                user.isVipActive(Instant.now()),
                                user.getVipExpiresAt());
        }

        public void logout(LogoutRequest request) {
                refreshTokenRepository.deleteByToken(request.refreshToken());
        }

        public User findUserByEmail(String email) {
                return userRepository.findByEmail(email)
                                .orElseThrow(() -> new DomainException("User not found"));
        }

        private AuthResponse generateAuthResponse(User user) {
                String token = jwtService.generateToken(user.getEmail(), user.getId(), user.getRole().name());
                RefreshToken refreshToken = createRefreshToken(user.getId());

                return new AuthResponse(
                                token,
                                refreshToken.getToken(),
                                user.getId(),
                                user.getName(),
                                user.getEmail(),
                                user.getRole(),
                                user.isVipActive(Instant.now()),
                                user.getVipExpiresAt());
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
