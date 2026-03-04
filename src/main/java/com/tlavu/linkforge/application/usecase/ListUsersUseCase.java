package com.tlavu.linkforge.application.usecase;

import com.tlavu.linkforge.application.dto.response.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ListUsersUseCase {
    Page<UserResponse> execute(String keyword, Pageable pageable);
}
