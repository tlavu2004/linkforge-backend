package com.tlavu.linkforge.infrastructure.adapter;

import com.tlavu.linkforge.domain.entity.User;
import com.tlavu.linkforge.domain.repository.UserRepository;
import com.tlavu.linkforge.infrastructure.persistence.entity.UserJpaEntity;
import com.tlavu.linkforge.infrastructure.persistence.mapper.UserMapper;
import com.tlavu.linkforge.infrastructure.persistence.repository.UserJpaRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@SuppressWarnings("null")
public class UserRepositoryAdapter implements UserRepository {

    private final UserJpaRepository userJpaRepository;
    private final UserMapper userMapper;

    @Override
    public User save(User user) {
        UserJpaEntity entity = userMapper.toJpaEntity(user);
        UserJpaEntity savedEntity = userJpaRepository.save(entity);
        return userMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userJpaRepository.findByEmail(email).map(userMapper::toDomain);
    }

    @Override
    public Optional<User> findById(Long id) {
        return userJpaRepository.findById(id).map(userMapper::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userJpaRepository.existsByEmail(email);
    }

    @Override
    public Page<User> searchUsers(String keyword, Pageable pageable) {
        if (keyword != null && !keyword.trim().isEmpty()) {
            String term = keyword.trim();
            return userJpaRepository.findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(term, term, pageable)
                    .map(userMapper::toDomain);
        }
        return userJpaRepository.findAll(pageable).map(userMapper::toDomain);
    }
}
