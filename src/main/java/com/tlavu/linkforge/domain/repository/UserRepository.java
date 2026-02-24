package com.tlavu.linkforge.domain.repository;

import com.tlavu.linkforge.domain.entity.User;
import java.util.Optional;

public interface UserRepository {
    User save(User user);

    Optional<User> findByEmail(String email);

    Optional<User> findById(Long id);

    boolean existsByEmail(String email);
}
