package com.tlavu.linkforge.application.dto;

import jakarta.validation.constraints.NotNull;

public record ToggleVipRequest(
                @NotNull(message = "vip status is required") Boolean vip) {
}
