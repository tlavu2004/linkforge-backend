package com.tlavu.linkforge.domain.entity;

import com.tlavu.linkforge.domain.exception.DomainException;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@EqualsAndHashCode(of = "id")
@NoArgsConstructor(force = true, access = AccessLevel.PRIVATE) // For MapStruct/Hibernate
public class User {

    private final Long id;
    private final String name;
    private final String email;
    private String passwordHash;
    private final Role role;
    private final Instant createdAt;

    private Boolean vip;
    private Boolean emailVerified;
    private Instant vipExpiresAt;
    private Instant updatedAt;

    public User(Long id, String name, String email, String passwordHash, Role role, Instant createdAt, Boolean vip,
            Boolean emailVerified, Instant vipExpiresAt, Instant updatedAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.createdAt = createdAt;
        this.vip = vip;
        this.emailVerified = emailVerified;
        this.vipExpiresAt = vipExpiresAt;
        this.updatedAt = updatedAt;
    }

    public static User create(Long id, String name, String email, String passwordHash, Role role) {
        if (id == null) {
            throw new DomainException("User ID must not be null");
        }
        if (name == null || name.isBlank()) {
            throw new DomainException("User name must not be null or blank");
        }
        if (email == null || email.isBlank()) {
            throw new DomainException("User email must not be null or blank");
        }
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new DomainException("User password hash must not be null or blank");
        }
        if (role == null) {
            throw new DomainException("User role must not be null");
        }

        Instant now = Instant.now();
        return new User(id, name, email, passwordHash, role, now, false, false, null, now);
    }

    public boolean isVip() {
        return vip != null && vip;
    }

    public boolean isEmailVerified() {
        return emailVerified != null && emailVerified;
    }

    public void verifyEmail() {
        this.emailVerified = true;
        this.updatedAt = Instant.now();
    }

    public void updatePassword(String newPasswordHash) {
        if (newPasswordHash == null || newPasswordHash.isBlank()) {
            throw new DomainException("Password hash must not be null or blank");
        }
        this.passwordHash = newPasswordHash;
        this.updatedAt = Instant.now();
    }

    public boolean isVipActive(Instant now) {
        if (!vip) {
            return false;
        }
        return vipExpiresAt == null || vipExpiresAt.isAfter(now);
    }

    public void grantLifetimeVip() {
        this.vip = true;
        this.vipExpiresAt = null;
        this.updatedAt = Instant.now();
    }

    public void revokeVip() {
        this.vip = false;
        this.vipExpiresAt = null;
        this.updatedAt = Instant.now();
    }

    public void grantTemporaryVip(Instant expirationDate) {
        if (expirationDate == null) {
            throw new DomainException("Expiration date must not be null for temporary VIP");
        }
        if (expirationDate.isBefore(Instant.now())) {
            throw new DomainException("Expiration date must be in the future");
        }
        this.vip = true;
        this.vipExpiresAt = expirationDate;
        this.updatedAt = Instant.now();
    }
}
