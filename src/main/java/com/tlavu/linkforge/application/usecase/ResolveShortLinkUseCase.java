package com.tlavu.linkforge.application.usecase;

import com.tlavu.linkforge.application.dto.ShortLinkResponse;

public interface ResolveShortLinkUseCase {
    ShortLinkResponse execute(String shortCode);
}
