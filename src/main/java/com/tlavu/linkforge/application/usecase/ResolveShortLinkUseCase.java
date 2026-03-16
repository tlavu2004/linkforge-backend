package com.tlavu.linkforge.application.usecase;

import com.tlavu.linkforge.application.dto.response.ShortLinkResponse;

public interface ResolveShortLinkUseCase {
    ShortLinkResponse execute(String shortCode, boolean isAdVerification, String ipAddress, String userAgent, String referrer);
}
