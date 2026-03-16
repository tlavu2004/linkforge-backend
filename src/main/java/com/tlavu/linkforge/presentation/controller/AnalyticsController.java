package com.tlavu.linkforge.presentation.controller;

import com.tlavu.linkforge.application.dto.response.LinkStatsResponse;
import com.tlavu.linkforge.application.usecase.GetLinkAnalyticsUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "Endpoints for link click analytics and statistics")
public class AnalyticsController {

    private final GetLinkAnalyticsUseCase getLinkAnalyticsUseCase;

    @GetMapping("/{shortCode}")
    @Operation(summary = "Get detailed analytics for a short link", description = "Returns click counts, unique visitors, geo distribution, and daily stats. Requires ownership or a valid delete token.")
    public LinkStatsResponse getAnalytics(
            @PathVariable String shortCode,
            @RequestParam(required = false) String token,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        
        if (to == null) to = Instant.now();
        if (from == null) from = to.minus(30, ChronoUnit.DAYS);

        return getLinkAnalyticsUseCase.execute(shortCode, token, from, to);
    }
}
