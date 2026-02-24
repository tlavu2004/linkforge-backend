package com.tlavu.linkforge.application.dto;

import com.tlavu.linkforge.domain.entity.Role;

public record AuthResponse(
                String accessToken,
                String refreshToken,
                Long userId,
                String email,
                Role role,
                boolean vip) {
}
