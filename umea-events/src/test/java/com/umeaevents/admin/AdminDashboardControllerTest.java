package com.umeaevents.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umeaevents.event.EventStatus;
import com.umeaevents.user.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = WebEnvironment.MOCK)
class AdminDashboardControllerTest {

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private AdminStatsService statsService;

    @MockitoBean
    private AdminEventService eventService;

    @MockitoBean
    private AdminUserService userService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    private AdminStatsResponse sampleStats() {
        return new AdminStatsResponse(
                10L,
                3L,
                new AdminStatsResponse.EventStats(2L, 1L, 5L, 0L, 0L),
                new AdminStatsResponse.ScrapedStats(4L, 1L, 2L),
                7L
        );
    }

    private AdminEventResponse sampleEvent() {
        return new AdminEventResponse(
                UUID.randomUUID(), "Pubquiz", null, null,
                UUID.randomUUID(), "Bishops Arms",
                UUID.randomUUID(), "Quiz",
                UUID.randomUUID(), "owner@test.com",
                EventStatus.PENDING_REVIEW.name(),
                false,
                OffsetDateTime.now(), OffsetDateTime.now()
        );
    }

    private AdminUserResponse sampleUser() {
        return new AdminUserResponse(UUID.randomUUID(), "user@test.com", Role.USER.name(), true, OffsetDateTime.now());
    }

    // ── Auth guards ───────────────────────────────────────────────────────────

    @Test
    void stats_noAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/stats"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void stats_userRole_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/stats"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "RESTAURANT")
    void stats_restaurantRole_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/stats"))
                .andExpect(status().isForbidden());
    }

    // ── Stats ─────────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void stats_adminRole_returns200WithCounts() throws Exception {
        when(statsService.stats()).thenReturn(sampleStats());

        mockMvc.perform(get("/api/v1/admin/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(10))
                .andExpect(jsonPath("$.totalVenues").value(3))
                .andExpect(jsonPath("$.events.pendingReview").value(1))
                .andExpect(jsonPath("$.events.published").value(5))
                .andExpect(jsonPath("$.scrapedEvents.pendingReview").value(4))
                .andExpect(jsonPath("$.upcomingOccurrences7Days").value(7));
    }

    // ── Events ────────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void listEvents_noFilter_returns200() throws Exception {
        var page = new PageImpl<>(List.of(sampleEvent()), PageRequest.of(0, 20), 1);
        when(eventService.listEvents(any(), any(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/admin/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Pubquiz"))
                .andExpect(jsonPath("$.content[0].status").value("PENDING_REVIEW"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listEvents_withStatusFilter_passesStatusToService() throws Exception {
        var page = new PageImpl<>(List.of(sampleEvent()), PageRequest.of(0, 20), 1);
        when(eventService.listEvents(eq(EventStatus.PENDING_REVIEW), any(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/admin/events").param("status", "PENDING_REVIEW"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    @WithMockUser(roles = "USER")
    void listEvents_userRole_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/events"))
                .andExpect(status().isForbidden());
    }

    // ── Users ─────────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void listUsers_returns200() throws Exception {
        var page = new PageImpl<>(List.of(sampleUser()), PageRequest.of(0, 20), 1);
        when(userService.listUsers(any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].email").value("user@test.com"))
                .andExpect(jsonPath("$.content[0].role").value("USER"));
    }

    @Test
    @WithMockUser(roles = "ADMIN", username = "admin@test.com")
    void changeRole_validRequest_returns200WithNewRole() throws Exception {
        UUID userId = UUID.randomUUID();
        var updated = new AdminUserResponse(userId, "user@test.com", Role.RESTAURANT.name(), true, OffsetDateTime.now());
        when(userService.changeRole(eq(userId), any(), eq("admin@test.com"))).thenReturn(updated);

        var body = new ChangeRoleRequest(Role.RESTAURANT);
        mockMvc.perform(patch("/api/v1/admin/users/" + userId + "/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("RESTAURANT"));
    }

    @Test
    @WithMockUser(roles = "ADMIN", username = "admin@test.com")
    void changeRole_missingRole_returns400() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(patch("/api/v1/admin/users/" + userId + "/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "USER")
    void changeRole_userRole_returns403() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/users/" + UUID.randomUUID() + "/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ChangeRoleRequest(Role.ADMIN))))
                .andExpect(status().isForbidden());
    }
}
