package com.tlavu.linkforge.application.dto;

import com.tlavu.linkforge.domain.entity.Role;

public record RegisterResponse(
        Long userId,
        String email,
        Role role,
        boolean vip) {
}
