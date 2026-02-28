package com.tlavu.linkforge.presentation.controller;

import com.tlavu.linkforge.application.dto.command.CreateShortLinkCommand;
import com.tlavu.linkforge.application.dto.response.ShortLinkResponse;
import com.tlavu.linkforge.application.usecase.CreateShortLinkUseCase;
import com.tlavu.linkforge.application.usecase.DeleteShortLinkUseCase;
import com.tlavu.linkforge.application.usecase.GetShortLinkUseCase;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/links")
@RequiredArgsConstructor
@Tag(name = "Short Links Management", description = "Endpoints for creating, retrieving, and deleting short links")
public class ShortLinkController {

    private final CreateShortLinkUseCase createShortLinkUseCase;
    private final GetShortLinkUseCase getShortLinkUseCase;
    private final DeleteShortLinkUseCase deleteShortLinkUseCase;

    @Operation(summary = "Create a new short link", description = "Submit a long URL to generate a unique short code.")
    @ApiResponse(responseCode = "201", description = "Short link created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request format or URL")
    @PostMapping
    public ResponseEntity<com.tlavu.linkforge.presentation.response.ApiResponse<ShortLinkResponse>> createShortLink(
            @Valid @RequestBody CreateShortLinkCommand command) {
        ShortLinkResponse response = createShortLinkUseCase.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(com.tlavu.linkforge.presentation.response.ApiResponse.success("Short link created successfully",
                        response));
    }

    @Operation(summary = "Get short link details", description = "Retrieve metadata and stats of a short link using its code.")
    @ApiResponse(responseCode = "200", description = "Link details found")
    @ApiResponse(responseCode = "404", description = "Short link not found")
    @GetMapping("/{shortCode}")
    public ResponseEntity<com.tlavu.linkforge.presentation.response.ApiResponse<ShortLinkResponse>> getShortLink(
            @Parameter(description = "The short code generated for the URL") @org.springframework.web.bind.annotation.PathVariable String shortCode) {
        ShortLinkResponse response = getShortLinkUseCase.execute(shortCode);
        return ResponseEntity.ok(com.tlavu.linkforge.presentation.response.ApiResponse.success(response));
    }

    @Operation(summary = "Delete a short link", description = "Soft delete a short link using its code and the secret delete token.")
    @ApiResponse(responseCode = "204", description = "Link deleted successfully")
    @ApiResponse(responseCode = "403", description = "Invalid delete token")
    @ApiResponse(responseCode = "404", description = "Short link not found")
    @DeleteMapping("/{shortCode}")
    public ResponseEntity<Void> deleteShortLink(
            @Parameter(description = "The short code of the link to delete") @org.springframework.web.bind.annotation.PathVariable String shortCode,
            @Parameter(description = "The secret token provided upon link creation") @org.springframework.web.bind.annotation.RequestParam String deleteToken) {
        deleteShortLinkUseCase.execute(shortCode, deleteToken);
        return ResponseEntity.noContent().build();
    }
}
