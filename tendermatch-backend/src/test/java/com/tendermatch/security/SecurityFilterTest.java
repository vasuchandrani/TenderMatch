package com.tendermatch.security;

import com.tendermatch.model.User;
import com.tendermatch.service.AuthService;
import com.tendermatch.service.TenderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private AuthService authService;

    @MockBean
    private TenderService tenderService;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    private String validJwtToken;
    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        UUID userId = UUID.randomUUID();
        User testUser = User.builder()
                .id(userId)
                .email("security_test@example.com")
                .name("Sec User")
                .passwordHash("password")
                .role("USER")
                .build();

        userDetails = new CustomUserDetails(testUser);
        validJwtToken = jwtService.generateToken(userDetails, userId);

        when(userDetailsService.loadUserByUsername("security_test@example.com")).thenReturn(userDetails);
    }

    @Test
    void shouldAllowAccessToAuthEndpoints() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"security_test@example.com\",\"password\":\"wrong\"}"))
                .andExpect(result -> {
                    // It should not be 401 from security filter blocking endpoint access;
                    // reaching controller/validation means auth endpoint is accessible
                    int status = result.getResponse().getStatus();
                    org.junit.jupiter.api.Assertions.assertNotEquals(403, status);
                });
    }

    @Test
    void shouldRejectProtectedEndpointsWithoutToken() throws Exception {
        mockMvc.perform(get("/api/tenders"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAcceptValidBearerToken() throws Exception {
        when(tenderService.listTenders(any(), any())).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/tenders")
                        .header("Authorization", "Bearer " + validJwtToken))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectInvalidBearerToken() throws Exception {
        String invalidToken = validJwtToken + "invalid-signature";

        mockMvc.perform(get("/api/tenders")
                        .header("Authorization", "Bearer " + invalidToken))
                .andExpect(status().isUnauthorized());
    }
}
