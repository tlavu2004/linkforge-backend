package com.tlavu.linkforge.application.dto.request;

import jakarta.validation.constraints.NotNull;

public record ToggleVipRequest(
                @NotNull(message = "vip status is required") Boolean vip) {
}
