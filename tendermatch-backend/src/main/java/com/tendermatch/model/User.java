package com.tendermatch.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, length = 50)
    private String role = "USER";

    @Column(name = "consent_training_data", nullable = false)
    private Boolean consentTrainingData = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedAt;

    public User() {}

    public User(UUID id, String email, String passwordHash, String name, String role, Boolean consentTrainingData, ZonedDateTime createdAt, ZonedDateTime updatedAt) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.name = name;
        this.role = role != null ? role : "USER";
        this.consentTrainingData = consentTrainingData != null ? consentTrainingData : false;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public User(UUID id, String email, String passwordHash, String name, String role, ZonedDateTime createdAt, ZonedDateTime updatedAt) {
        this(id, email, passwordHash, name, role, false, createdAt, updatedAt);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id;
        private String email;
        private String passwordHash;
        private String name;
        private String role = "USER";
        private Boolean consentTrainingData = false;
        private ZonedDateTime createdAt;
        private ZonedDateTime updatedAt;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder email(String email) { this.email = email; return this; }
        public Builder passwordHash(String passwordHash) { this.passwordHash = passwordHash; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder role(String role) { this.role = role; return this; }
        public Builder consentTrainingData(Boolean consentTrainingData) { this.consentTrainingData = consentTrainingData; return this; }
        public Builder createdAt(ZonedDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(ZonedDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public User build() {
            return new User(id, email, passwordHash, name, role, consentTrainingData, createdAt, updatedAt);
        }
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public Boolean getConsentTrainingData() { return consentTrainingData; }
    public void setConsentTrainingData(Boolean consentTrainingData) { this.consentTrainingData = consentTrainingData; }

    public ZonedDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(ZonedDateTime createdAt) { this.createdAt = createdAt; }

    public ZonedDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(ZonedDateTime updatedAt) { this.updatedAt = updatedAt; }
}
