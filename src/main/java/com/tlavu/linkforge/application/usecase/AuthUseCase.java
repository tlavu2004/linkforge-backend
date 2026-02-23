package com.tlavu.linkforge.application.usecase;

import io.hypersistence.tsid.TSID;
import com.tlavu.linkforge.application.dto.AuthResponse;
import com.tlavu.linkforge.application.dto.LoginRequest;
import com.tlavu.linkforge.application.dto.RegisterRequest;
import com.tlavu.linkforge.domain.entity.Role;
import com.tlavu.linkforge.domain.entity.User;
import com.tlavu.linkforge.domain.exception.DomainException;
import com.tlavu.linkforge.domain.repository.UserRepository;
import com.tlavu.linkforge.infrastructure.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthResponse register(RegisterRequest request) {
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

        String token = jwtService.generateToken(savedUser.getEmail(), savedUser.getId(), savedUser.getRole().name());

        return new AuthResponse(
                token,
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

        String token = jwtService.generateToken(user.getEmail(), user.getId(), user.getRole().name());

        return new AuthResponse(
                token,
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.isVipActive(Instant.now()));
    }
}
