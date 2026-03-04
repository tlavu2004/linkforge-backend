package com.tlavu.linkforge.presentation.controller;

import com.tlavu.linkforge.application.dto.response.UserLinkResponse;
import com.tlavu.linkforge.application.usecase.ListUserLinksUseCase;
import com.tlavu.linkforge.presentation.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import com.tlavu.linkforge.domain.repository.ShortLinkRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/users/{userId}/links")
@RequiredArgsConstructor
@Tag(name = "Admin User Links Management", description = "Endpoints for managing specific user links as admin")
public class AdminUserLinkController {

    private final ListUserLinksUseCase listUserLinksUseCase;
    private final ShortLinkRepository shortLinkRepository;

    @GetMapping
    @Operation(summary = "List user links", description = "Returns paginated list of a specific user's links")
    public ResponseEntity<ApiResponse<Page<UserLinkResponse>>> getUserLinks(
            @PathVariable Long userId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        String sortField = mapSortField(sortBy);
        Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        PageRequest pageable = PageRequest.of(page, Math.min(size, 50), Sort.by(sortDirection, sortField));

        Page<UserLinkResponse> links = listUserLinksUseCase.execute(userId, keyword, pageable);
        return ResponseEntity.ok(ApiResponse.success(links));
    }

    @DeleteMapping("/{shortCode}")
    @Operation(summary = "Delete user link", description = "Deletes a specific link owned by the user (as admin)")
    public ResponseEntity<ApiResponse<Void>> deleteUserLink(
            @PathVariable Long userId,
            @PathVariable String shortCode) {

        com.tlavu.linkforge.domain.entity.ShortLink link = shortLinkRepository
                .findByShortCode(com.tlavu.linkforge.domain.valueobject.ShortCode.of(shortCode))
                .orElseThrow(() -> new com.tlavu.linkforge.domain.exception.DomainException("Short link not found"));

        if (link.getUserId() == null || !link.getUserId().equals(userId)) {
            throw new com.tlavu.linkforge.domain.exception.DomainException(
                    "This link does not belong to the specified user");
        }

        shortLinkRepository.delete(link.getId());
        return ResponseEntity.ok(ApiResponse.success("Link deleted successfully", null));
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
