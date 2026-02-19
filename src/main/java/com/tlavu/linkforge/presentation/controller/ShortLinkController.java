package com.tlavu.linkforge.presentation.controller;

import com.tlavu.linkforge.application.dto.CreateShortLinkCommand;
import com.tlavu.linkforge.application.dto.ShortLinkResponse;
import com.tlavu.linkforge.application.usecase.CreateShortLinkUseCase;
import com.tlavu.linkforge.presentation.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/links")
@RequiredArgsConstructor
public class ShortLinkController {

    private final CreateShortLinkUseCase createShortLinkUseCase;

    @PostMapping
    public ResponseEntity<ApiResponse<ShortLinkResponse>> createShortLink(
            @Valid @RequestBody CreateShortLinkCommand command) {
        ShortLinkResponse response = createShortLinkUseCase.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Short link created successfully", response));
    }
}
