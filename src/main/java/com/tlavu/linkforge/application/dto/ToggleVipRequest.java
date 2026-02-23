package com.tlavu.linkforge.application.dto;

import jakarta.validation.constraints.NotNull;

public record ToggleVipRequest(
        @NotNull(message = "isVip status is required") Boolean isVip) {
}
