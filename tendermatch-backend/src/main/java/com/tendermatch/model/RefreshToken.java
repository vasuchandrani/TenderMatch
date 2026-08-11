package com.tendermatch.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token_hash", nullable = false, unique = true, length = 255)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private ZonedDateTime expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    @Column(nullable = false)
    private Boolean revoked = false;

    public RefreshToken() {}

    public RefreshToken(UUID id, User user, String tokenHash, ZonedDateTime expiresAt, ZonedDateTime createdAt, Boolean revoked) {
        this.id = id;
        this.user = user;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
        this.revoked = revoked != null ? revoked : false;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id;
        private User user;
        private String tokenHash;
        private ZonedDateTime expiresAt;
        private ZonedDateTime createdAt;
        private Boolean revoked = false;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder user(User user) { this.user = user; return this; }
        public Builder tokenHash(String tokenHash) { this.tokenHash = tokenHash; return this; }
        public Builder expiresAt(ZonedDateTime expiresAt) { this.expiresAt = expiresAt; return this; }
        public Builder createdAt(ZonedDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder revoked(Boolean revoked) { this.revoked = revoked; return this; }

        public RefreshToken build() {
            return new RefreshToken(id, user, tokenHash, expiresAt, createdAt, revoked);
        }
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }

    public ZonedDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(ZonedDateTime expiresAt) { this.expiresAt = expiresAt; }

    public ZonedDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(ZonedDateTime createdAt) { this.createdAt = createdAt; }

    public Boolean getRevoked() { return revoked; }
    public void setRevoked(Boolean revoked) { this.revoked = revoked; }
}
