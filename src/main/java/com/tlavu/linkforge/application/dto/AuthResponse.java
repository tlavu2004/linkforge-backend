package com.tlavu.linkforge.application.dto;

import com.tlavu.linkforge.domain.entity.Role;

public record AuthResponse(
                String token,
                Long userId,
                String email,
                Role role,
                boolean vip) {
}
