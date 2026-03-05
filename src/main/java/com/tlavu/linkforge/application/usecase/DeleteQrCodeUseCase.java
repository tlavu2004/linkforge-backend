package com.tlavu.linkforge.application.usecase;

import com.tlavu.linkforge.application.dto.response.ShortLinkResponse;

public interface DeleteQrCodeUseCase {
    ShortLinkResponse execute(String shortCode, Long userId);
}
