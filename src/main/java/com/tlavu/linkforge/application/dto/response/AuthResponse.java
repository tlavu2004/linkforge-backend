package com.tlavu.linkforge.application.dto.response;

import com.tlavu.linkforge.domain.entity.Role;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        Long userId,
        String name,
        String email,
        Role role,
        boolean vip) {
}
