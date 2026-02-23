package com.tlavu.linkforge.presentation.controller;

import com.tlavu.linkforge.presentation.response.ApiResponse;
import com.tlavu.linkforge.application.dto.ToggleVipRequest;
import com.tlavu.linkforge.application.usecase.ToggleVipStatusUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final ToggleVipStatusUseCase toggleVipStatusUseCase;

    @PostMapping("/{userId}/vip/toggle")
    public ResponseEntity<ApiResponse<Void>> toggleVip(
            @PathVariable Long userId,
            @RequestBody @Valid ToggleVipRequest request) {
        toggleVipStatusUseCase.execute(userId, request.vip());
        return ResponseEntity.ok(ApiResponse.success("User VIP status updated successfully", null));
    }
}
