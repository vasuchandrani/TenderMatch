package com.tendermatch.service;

import com.tendermatch.dto.auth.AuthResponse;
import com.tendermatch.dto.auth.LoginRequest;
import com.tendermatch.dto.auth.RefreshTokenRequest;
import com.tendermatch.dto.auth.RegisterRequest;
import com.tendermatch.dto.auth.UserDto;
import com.tendermatch.exception.BadRequestException;
import com.tendermatch.model.RefreshToken;
import com.tendermatch.model.User;
import com.tendermatch.repository.RefreshTokenRepository;
import com.tendermatch.repository.UserRepository;
import com.tendermatch.security.CustomUserDetails;
import com.tendermatch.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private User sampleUser;
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "refreshExpirationMs", 604800000L);

        sampleUser = User.builder()
                .id(userId)
                .name("Alice Corp")
                .email("alice@example.com")
                .passwordHash("encoded_secret_hash")
                .role("USER")
                .build();
    }

    @Test
    void shouldRegisterNewUser() {
        RegisterRequest request = new RegisterRequest("Alice Corp", "alice@example.com", "password123");

        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded_secret_hash");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        when(jwtService.generateToken(any(CustomUserDetails.class), eq(userId))).thenReturn("jwt_access_token");
        when(jwtService.getExpirationMs()).thenReturn(3600000L);

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("jwt_access_token", response.getAccessToken());
        assertNotNull(response.getRefreshToken());
        assertEquals("alice@example.com", response.getUser().getEmail());
        assertEquals("Alice Corp", response.getUser().getName());
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void shouldRejectDuplicateEmail() {
        RegisterRequest request = new RegisterRequest("Alice Corp", "alice@example.com", "password123");
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> authService.register(request));
        assertTrue(ex.getMessage().contains("Email already registered"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldHashPasswordOnRegister() {
        RegisterRequest request = new RegisterRequest("Bob", "bob@example.com", "myPlainPassword");

        when(userRepository.existsByEmail("bob@example.com")).thenReturn(false);
        when(passwordEncoder.encode("myPlainPassword")).thenReturn("hashed_bob_pw");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });
        when(jwtService.generateToken(any(CustomUserDetails.class), any())).thenReturn("mock_token");

        authService.register(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals("hashed_bob_pw", captor.getValue().getPasswordHash());
    }

    @Test
    void shouldLoginWithValidCredentials() {
        LoginRequest request = new LoginRequest("alice@example.com", "password123");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken("alice@example.com", "password123"));
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(sampleUser));
        when(jwtService.generateToken(any(CustomUserDetails.class), eq(userId))).thenReturn("jwt_access_token");
        when(jwtService.getExpirationMs()).thenReturn(3600000L);

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("jwt_access_token", response.getAccessToken());
        assertEquals("alice@example.com", response.getUser().getEmail());
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void shouldRejectInvalidCredentials() {
        LoginRequest request = new LoginRequest("alice@example.com", "wrongPassword");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class, () -> authService.login(request));
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    @Test
    void shouldRefreshToken() {
        String oldTokenString = "old_valid_refresh_token";
        RefreshToken existingToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .tokenHash(oldTokenString)
                .expiresAt(ZonedDateTime.now().plusDays(5))
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByTokenHash(oldTokenString)).thenReturn(Optional.of(existingToken));
        when(jwtService.generateToken(any(CustomUserDetails.class), eq(userId))).thenReturn("new_access_token");
        when(jwtService.getExpirationMs()).thenReturn(3600000L);

        RefreshTokenRequest request = new RefreshTokenRequest(oldTokenString);
        AuthResponse response = authService.refreshToken(request);

        assertNotNull(response);
        assertEquals("new_access_token", response.getAccessToken());
        assertTrue(existingToken.getRevoked());
        // Verify old token was saved as revoked and new token saved
        verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class));
    }

    @Test
    void shouldRejectExpiredOrRevokedRefreshToken() {
        RefreshToken revokedToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .tokenHash("revoked_token")
                .expiresAt(ZonedDateTime.now().plusDays(5))
                .revoked(true)
                .build();

        when(refreshTokenRepository.findByTokenHash("revoked_token")).thenReturn(Optional.of(revokedToken));

        assertThrows(BadRequestException.class, () -> authService.refreshToken(new RefreshTokenRequest("revoked_token")));

        RefreshToken expiredToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .tokenHash("expired_token")
                .expiresAt(ZonedDateTime.now().minusDays(1))
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByTokenHash("expired_token")).thenReturn(Optional.of(expiredToken));

        assertThrows(BadRequestException.class, () -> authService.refreshToken(new RefreshTokenRequest("expired_token")));
    }
}
