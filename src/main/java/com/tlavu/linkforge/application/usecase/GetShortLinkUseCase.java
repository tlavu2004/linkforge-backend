package com.tlavu.linkforge.application.usecase;

import com.tlavu.linkforge.application.dto.response.ShortLinkResponse;

public interface GetShortLinkUseCase {
    ShortLinkResponse execute(String shortCode);
}
