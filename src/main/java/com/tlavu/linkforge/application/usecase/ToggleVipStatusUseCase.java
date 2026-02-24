package com.tlavu.linkforge.application.usecase;

import com.tlavu.linkforge.domain.entity.User;
import com.tlavu.linkforge.domain.exception.DomainException;
import com.tlavu.linkforge.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ToggleVipStatusUseCase {

    private final UserRepository userRepository;

    public void execute(Long userId, boolean vip) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new DomainException("User not found"));

        if (vip) {
            user.grantLifetimeVip();
        } else {
            user.revokeVip();
        }

        userRepository.save(user);
    }
}
