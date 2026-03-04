package com.tlavu.linkforge.presentation.controller;

import com.tlavu.linkforge.presentation.response.ApiResponse;
import com.tlavu.linkforge.application.dto.request.ToggleVipRequest;
import com.tlavu.linkforge.application.usecase.ToggleVipStatusUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.tlavu.linkforge.application.dto.response.UserResponse;
import com.tlavu.linkforge.application.usecase.ListUsersUseCase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@Tag(name = "Admin User Management", description = "Endpoints for managing user accounts and roles")
public class AdminUserController {

    private final ToggleVipStatusUseCase toggleVipStatusUseCase;
    private final ListUsersUseCase listUsersUseCase;

    @GetMapping
    @Operation(summary = "List users", description = "Returns an admin paginated list of all users")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> getUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        String sortField = mapSortField(sortBy);
        Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        PageRequest pageable = PageRequest.of(page, Math.min(size, 50), Sort.by(sortDirection, sortField));

        Page<UserResponse> users = listUsersUseCase.execute(keyword, pageable);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    private String mapSortField(String sortBy) {
        return switch (sortBy.toLowerCase()) {
            case "username", "name" -> "username";
            case "email" -> "email";
            case "id" -> "id";
            case "role" -> "role";
            default -> "createdAt";
        };
    }

    @PostMapping("/{userId}/vip/toggle")
    public ResponseEntity<ApiResponse<Void>> toggleVip(
            @PathVariable Long userId,
            @RequestBody @Valid ToggleVipRequest request) {
        toggleVipStatusUseCase.execute(userId, request.vip());
        return ResponseEntity.ok(ApiResponse.success("User VIP status updated successfully", null));
    }
}
