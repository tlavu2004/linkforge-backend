package com.tlavu.linkforge.application.usecase;

import com.tlavu.linkforge.application.dto.response.LinkStatsResponse;
import java.time.Instant;

public interface GetLinkAnalyticsUseCase {
    LinkStatsResponse execute(String shortCode, String token, Instant from, Instant to);
}
