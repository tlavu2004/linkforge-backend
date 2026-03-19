package com.tlavu.linkforge.presentation.controller;

import com.tlavu.linkforge.application.dto.response.UserLinkResponse;
import com.tlavu.linkforge.application.usecase.DeleteQrCodeUseCase;
import com.tlavu.linkforge.application.usecase.GenerateQrCodeUseCase;
import com.tlavu.linkforge.application.usecase.ListUserLinksUseCase;
import com.tlavu.linkforge.application.dto.response.ShortLinkResponse;
import com.tlavu.linkforge.domain.entity.ShortLink;
import com.tlavu.linkforge.domain.exception.DomainException;
import com.tlavu.linkforge.domain.repository.ShortLinkRepository;
import com.tlavu.linkforge.domain.valueobject.ShortCode;
import com.tlavu.linkforge.infrastructure.security.JwtService;
import com.tlavu.linkforge.presentation.response.ApiResponse;
import com.tlavu.linkforge.shared.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/me/links")
@RequiredArgsConstructor
@Tag(name = "My Links", description = "Manage authenticated user's links")
@SuppressWarnings("null")
public class UserLinkController {

    private final ListUserLinksUseCase listUserLinksUseCase;
    private final GenerateQrCodeUseCase generateQrCodeUseCase;
    private final DeleteQrCodeUseCase deleteQrCodeUseCase;
    private final ShortLinkRepository shortLinkRepository;
    private final JwtService jwtService;
    private final MessageService messageService;

    @GetMapping
    @Operation(summary = "List my links", description = "Returns paginated list of the authenticated user's links with sorting")
    public ResponseEntity<ApiResponse<Page<UserLinkResponse>>> getMyLinks(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Long userId = extractUserId(authHeader);

        // Validate and map sort fields
        String sortField = mapSortField(sortBy);
        Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        PageRequest pageable = PageRequest.of(page, Math.min(size, 50), Sort.by(sortDirection, sortField));

        Page<UserLinkResponse> links = listUserLinksUseCase.execute(userId, keyword, pageable);
        return ResponseEntity.ok(ApiResponse.success(links));
    }

    @DeleteMapping("/{shortCode}")
    @Operation(summary = "Delete my link", description = "Deletes a link owned by the authenticated user (no delete token needed)")
    public ResponseEntity<ApiResponse<Void>> deleteMyLink(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String shortCode,
            java.util.Locale locale) {

        Long userId = extractUserId(authHeader);

        ShortLink link = shortLinkRepository.findByShortCode(ShortCode.of(shortCode))
                .orElseThrow(() -> new DomainException("shortlink.not_found"));

        if (link.getUserId() == null || !link.getUserId().equals(userId)) {
            throw new DomainException("link.owner_only");
        }

        shortLinkRepository.delete(link.getId());
        return ResponseEntity.ok(ApiResponse.success(messageService.getMessage("link.delete_success", locale), null));
    }

    @PostMapping("/{shortCode}/qr-code")
    @Operation(summary = "Generate QR code", description = "Generates a QR code for the link. Required VIP or Admin status.")
    public ResponseEntity<ApiResponse<ShortLinkResponse>> generateQrCode(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String shortCode,
            java.util.Locale locale) {

        Long userId = extractUserId(authHeader);
        ShortLinkResponse response = generateQrCodeUseCase.execute(shortCode, userId);
        return ResponseEntity.ok(ApiResponse.success(messageService.getMessage("qr.generate_success", locale), response));
    }

    @DeleteMapping("/{shortCode}/qr-code")
    @Operation(summary = "Delete QR code", description = "Deletes the stored QR code for the link.")
    public ResponseEntity<ApiResponse<ShortLinkResponse>> deleteQrCode(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String shortCode,
            java.util.Locale locale) {

        Long userId = extractUserId(authHeader);
        ShortLinkResponse response = deleteQrCodeUseCase.execute(shortCode, userId);
        return ResponseEntity.ok(ApiResponse.success(messageService.getMessage("qr.delete_success", locale), response));
    }

    private Long extractUserId(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return jwtService.extractUserId(token);
    }

    private String mapSortField(String sortBy) {
        return switch (sortBy.toLowerCase()) {
            case "originalurl", "url", "name" -> "originalUrl";
            case "createdat", "created" -> "createdAt";
            case "expiresat", "expires" -> "expiresAt";
            case "clickcount", "clicks" -> "clickCount";
            default -> "createdAt";
        };
    }
}
