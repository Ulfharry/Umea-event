package com.umeaevents.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umeaevents.auth.dto.LoginRequest;
import com.umeaevents.auth.dto.MeResponse;
import com.umeaevents.auth.dto.RegisterRequest;
import com.umeaevents.auth.dto.TokenResponse;
import com.umeaevents.user.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = WebEnvironment.MOCK)
class AuthControllerTest {

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private AuthService authService;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void register_validRequest_returns201AndTokens() throws Exception {
        var request = new RegisterRequest("test@example.com", "password123", Role.USER);
        var response = new TokenResponse("access.token.here", "refresh-token-uuid", 900000L);
        when(authService.register(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("access.token.here"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token-uuid"));
    }

    @Test
    void register_missingEmail_returns400() throws Exception {
        var request = new RegisterRequest("", "password123", Role.USER);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_validCredentials_returns200AndTokens() throws Exception {
        var request = new LoginRequest("test@example.com", "password123");
        var response = new TokenResponse("access.token.here", "refresh-token-uuid", 900000L);
        when(authService.login(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists());
    }

    @Test
    void refresh_validToken_returns200AndNewTokens() throws Exception {
        var request = new com.umeaevents.auth.dto.RefreshRequest("valid-refresh-token");
        var response = new TokenResponse("new.access.token", "new-refresh-uuid", 900000L);
        when(authService.refresh(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new.access.token"));
    }

    @Test
    void login_wrongPassword_returns401() throws Exception {
        var request = new LoginRequest("test@example.com", "wrong");
        when(authService.login(any()))
                .thenThrow(new org.springframework.security.authentication.BadCredentialsException("bad"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void me_noAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "owner@test.com", roles = "RESTAURANT")
    void me_authenticated_returnsUser() throws Exception {
        var me = new MeResponse(UUID.randomUUID(), "owner@test.com", Role.RESTAURANT);
        when(authService.me(eq("owner@test.com"))).thenReturn(me);

        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("owner@test.com"))
                .andExpect(jsonPath("$.role").value("RESTAURANT"));
    }
}
