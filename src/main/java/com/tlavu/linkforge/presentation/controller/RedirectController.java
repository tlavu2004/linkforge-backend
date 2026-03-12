package com.tlavu.linkforge.presentation.controller;

import com.tlavu.linkforge.application.dto.response.ShortLinkResponse;
import com.tlavu.linkforge.application.usecase.GenerateAdTokenUseCase;
import com.tlavu.linkforge.application.usecase.ResolveShortLinkUseCase;
import com.tlavu.linkforge.domain.exception.ShortLinkExpiredException;
import com.tlavu.linkforge.domain.exception.ShortLinkNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.beans.factory.annotation.Value;

@RestController
@RequestMapping
@RequiredArgsConstructor
@Tag(name = "Redirection", description = "Endpoint for redirecting short links to their original URLs")
@SuppressWarnings("null")
public class RedirectController {

    private final ResolveShortLinkUseCase resolveShortLinkUseCase;
    private final GenerateAdTokenUseCase generateAdTokenUseCase;

    @Value("${application.frontend.url}")
    private String frontendUrl;

    @Operation(summary = "Redirect to Original URL", description = "Takes a short code and redirects the client with a 301 status code to the target original URL.")
    @ApiResponse(responseCode = "301", description = "Redirecting to original URL")
    @ApiResponse(responseCode = "404", description = "Short link not found")
    @ApiResponse(responseCode = "410", description = "Short link expired")
    @GetMapping("/r/{shortCode}")
    public ResponseEntity<Void> redirect(
            @Parameter(description = "The short code generated for the URL") @PathVariable String shortCode) {
        try {
            ShortLinkResponse response = resolveShortLinkUseCase.execute(shortCode, false);

            if (response.skipAds()) {
                // VIP: 301 Permanent Redirect — CDN can cache this for 24h
                return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY)
                        .location(URI.create(response.originalUrl()))
                        .cacheControl(CacheControl.maxAge(java.time.Duration.ofDays(1)).cachePublic())
                        .header(HttpHeaders.VARY, "Accept")
                        .build();
            } else {
                // Non-VIP: 302 via ad buffer — must NOT be cached (unique token per request)
                String adToken = generateAdTokenUseCase.execute(shortCode);
                String bufferPageUrl = String.format("%s/buffer?code=%s&token=%s", frontendUrl, shortCode, adToken);
                return ResponseEntity.status(HttpStatus.FOUND)
                        .location(URI.create(bufferPageUrl))
                        .cacheControl(CacheControl.noStore())
                        .header(HttpHeaders.VARY, "Accept")
                        .build();
            }
        } catch (ShortLinkExpiredException e) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(frontendUrl + "/expired"))
                    .cacheControl(CacheControl.noStore())
                    .build();
        } catch (ShortLinkNotFoundException e) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(frontendUrl + "/404"))
                    .cacheControl(CacheControl.noStore())
                    .build();
        }
    }
}
