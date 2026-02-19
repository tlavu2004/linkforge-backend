package com.tlavu.linkforge.presentation.controller;

import com.tlavu.linkforge.application.dto.ShortLinkResponse;
import com.tlavu.linkforge.application.usecase.ResolveShortLinkUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class RedirectController {

    private final ResolveShortLinkUseCase resolveShortLinkUseCase;

    @GetMapping("/r/{shortCode}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode) {
        ShortLinkResponse response = resolveShortLinkUseCase.execute(shortCode);
        return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY)
                .location(URI.create(response.originalUrl()))
                .build();
    }
}
