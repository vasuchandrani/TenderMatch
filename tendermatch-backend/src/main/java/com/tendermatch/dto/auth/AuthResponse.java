package com.tendermatch.dto.auth;

public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private long expiresInMs;
    private UserDto user;

    public AuthResponse() {}

    public AuthResponse(String accessToken, String refreshToken, String tokenType, long expiresInMs, UserDto user) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.tokenType = tokenType;
        this.expiresInMs = expiresInMs;
        this.user = user;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String accessToken;
        private String refreshToken;
        private String tokenType = "Bearer";
        private long expiresInMs;
        private UserDto user;

        public Builder accessToken(String accessToken) { this.accessToken = accessToken; return this; }
        public Builder refreshToken(String refreshToken) { this.refreshToken = refreshToken; return this; }
        public Builder tokenType(String tokenType) { this.tokenType = tokenType; return this; }
        public Builder expiresInMs(long expiresInMs) { this.expiresInMs = expiresInMs; return this; }
        public Builder user(UserDto user) { this.user = user; return this; }

        public AuthResponse build() {
            return new AuthResponse(accessToken, refreshToken, tokenType, expiresInMs, user);
        }
    }

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }

    public String getTokenType() { return tokenType; }
    public void setTokenType(String tokenType) { this.tokenType = tokenType; }

    public long getExpiresInMs() { return expiresInMs; }
    public void setExpiresInMs(long expiresInMs) { this.expiresInMs = expiresInMs; }

    public UserDto getUser() { return user; }
    public void setUser(UserDto user) { this.user = user; }
}
