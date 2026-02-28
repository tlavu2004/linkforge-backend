package com.tlavu.linkforge.application.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreatePaymentLinkRequest(
        @NotBlank(message = "Package code is required") String packageCode) {
}
