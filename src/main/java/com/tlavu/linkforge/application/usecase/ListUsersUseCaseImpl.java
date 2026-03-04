package com.tlavu.linkforge.application.usecase;

import com.tlavu.linkforge.application.dto.response.UserResponse;
import com.tlavu.linkforge.domain.entity.User;
import com.tlavu.linkforge.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ListUsersUseCaseImpl implements ListUsersUseCase {

    private final UserRepository userRepository;

    @Override
    public Page<UserResponse> execute(String keyword, Pageable pageable) {
        return userRepository.searchUsers(keyword, pageable).map(this::toResponse);
    }

    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .isVip(user.isVipActive(Instant.now()))
                .vipExpiresAt(user.getVipExpiresAt())
                .build();
    }
}
