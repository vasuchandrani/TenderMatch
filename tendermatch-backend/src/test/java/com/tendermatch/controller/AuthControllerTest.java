package com.tendermatch.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tendermatch.dto.auth.AuthResponse;
import com.tendermatch.dto.auth.LoginRequest;
import com.tendermatch.dto.auth.RefreshTokenRequest;
import com.tendermatch.dto.auth.RegisterRequest;
import com.tendermatch.dto.auth.UserDto;
import com.tendermatch.security.CustomUserDetailsService;
import com.tendermatch.security.JwtService;
import com.tendermatch.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false) // controller layer testing
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @Test
    void shouldRegisterNewUser() throws Exception {
        RegisterRequest request = new RegisterRequest("Test Company", "company@example.com", "secret123");
        AuthResponse response = AuthResponse.builder()
                .accessToken("token_123")
                .refreshToken("refresh_123")
                .tokenType("Bearer")
                .expiresInMs(3600000L)
                .user(UserDto.builder()
                        .id(UUID.randomUUID())
                        .email("company@example.com")
                        .name("Test Company")
                        .role("USER")
                        .build())
                .build();

        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("token_123"))
                .andExpect(jsonPath("$.data.user.email").value("company@example.com"));
    }

    @Test
    void shouldRejectRegisterWithMissingFields() throws Exception {
        RegisterRequest invalidRequest = new RegisterRequest("", "not-an-email", "123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void shouldLoginSuccessfully() throws Exception {
        LoginRequest request = new LoginRequest("company@example.com", "secret123");
        AuthResponse response = AuthResponse.builder()
                .accessToken("token_abc")
                .refreshToken("refresh_abc")
                .tokenType("Bearer")
                .expiresInMs(3600000L)
                .user(UserDto.builder()
                        .id(UUID.randomUUID())
                        .email("company@example.com")
                        .name("Test Company")
                        .role("USER")
                        .build())
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("token_abc"));
    }

    @Test
    void shouldRejectLoginWithWrongPassword() throws Exception {
        LoginRequest request = new LoginRequest("company@example.com", "wrongPass");

        when(authService.login(any(LoginRequest.class))).thenThrow(new BadCredentialsException("Invalid email or password"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void shouldRefreshTokenSuccessfully() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest("valid_refresh_token");
        AuthResponse response = AuthResponse.builder()
                .accessToken("new_access_token")
                .refreshToken("new_refresh_token")
                .tokenType("Bearer")
                .expiresInMs(3600000L)
                .user(UserDto.builder()
                        .id(UUID.randomUUID())
                        .email("company@example.com")
                        .name("Test Company")
                        .role("USER")
                        .build())
                .build();

        when(authService.refreshToken(any(RefreshTokenRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("new_access_token"));
    }

    @Test
    @WithMockUser(username = "company@example.com")
    void shouldGetCurrentUserWhenAuthenticated() throws Exception {
        UserDto userDto = UserDto.builder()
                .id(UUID.randomUUID())
                .email("company@example.com")
                .name("Test Company")
                .role("USER")
                .build();

        when(authService.getCurrentUser("company@example.com")).thenReturn(userDto);

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("company@example.com"));
    }

    @Test
    void shouldReturnUnauthorizedWhenNoPrincipalOnMeEndpoint() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }
}
