package com.umeaevents.scraping;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
class AdminScrapedEventControllerTest {

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private ScrapedEventService scrapedEventService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    private ScrapedEventResponse sampleResponse() {
        return new ScrapedEventResponse(
                UUID.randomUUID(),
                ScrapedEventSource.MANUAL_IMPORT.name(),
                null,
                "Pubquiz på Bishops Arms",
                "Rolig kväll med frågor",
                "Bishops Arms",
                "2026-07-01T19:00:00+02:00",
                null,
                OffsetDateTime.parse("2026-07-01T17:00:00Z"),
                null,
                ScrapedEventStatus.PENDING_REVIEW.name(),
                null,
                null,
                null,
                null,
                OffsetDateTime.now()
        );
    }

    // ── Auth checks ──────────────────────────────────────────────────────────

    @Test
    void list_noAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/scraped-events"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void list_userRole_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/scraped-events"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "RESTAURANT")
    void list_restaurantRole_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/scraped-events"))
                .andExpect(status().isForbidden());
    }

    // ── Happy-path ───────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN", username = "admin@test.com")
    void list_adminRole_returns200() throws Exception {
        var page = new PageImpl<>(List.of(sampleResponse()), PageRequest.of(0, 20), 1);
        when(scrapedEventService.list(any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/admin/scraped-events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].rawTitle").value("Pubquiz på Bishops Arms"));
    }

    @Test
    @WithMockUser(roles = "ADMIN", username = "admin@test.com")
    void list_filterByStatus_passesStatusToService() throws Exception {
        var page = new PageImpl<>(List.of(sampleResponse()), PageRequest.of(0, 20), 1);
        when(scrapedEventService.list(eq(ScrapedEventStatus.PENDING_REVIEW), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/admin/scraped-events")
                        .param("status", "PENDING_REVIEW"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    @WithMockUser(roles = "ADMIN", username = "admin@test.com")
    void import_validRequest_returns201() throws Exception {
        var request = new ImportScrapedEventRequest(
                "Pubquiz på Bishops Arms",
                "Rolig kväll med frågor",
                "Bishops Arms",
                "2026-07-01T19:00:00+02:00",
                null,
                "2026-07-01T17:00:00Z",
                null
        );
        when(scrapedEventService.importManual(any())).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/v1/admin/scraped-events/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rawTitle").value("Pubquiz på Bishops Arms"))
                .andExpect(jsonPath("$.status").value("PENDING_REVIEW"));
    }

    @Test
    @WithMockUser(roles = "ADMIN", username = "admin@test.com")
    void import_missingTitle_returns400() throws Exception {
        var request = new ImportScrapedEventRequest(
                "", null, null, null, null, null, null
        );

        mockMvc.perform(post("/api/v1/admin/scraped-events/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN", username = "admin@test.com")
    void reject_returns200WithRejectedStatus() throws Exception {
        UUID id = UUID.randomUUID();
        var rejected = new ScrapedEventResponse(
                id, ScrapedEventSource.MANUAL_IMPORT.name(), null,
                "Pubquiz", null, null, null, null, null, null,
                ScrapedEventStatus.REJECTED.name(), "Dublett", OffsetDateTime.now(),
                UUID.randomUUID(), null, OffsetDateTime.now()
        );
        when(scrapedEventService.reject(eq(id), any(), eq("admin@test.com"))).thenReturn(rejected);

        var body = new RejectScrapedEventRequest("Dublett");
        mockMvc.perform(post("/api/v1/admin/scraped-events/" + id + "/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.adminNotes").value("Dublett"));
    }

    @Test
    @WithMockUser(roles = "ADMIN", username = "admin@test.com")
    void promote_returns200WithPromotedStatus() throws Exception {
        UUID id = UUID.randomUUID();
        UUID promotedEventId = UUID.randomUUID();
        var promoted = new ScrapedEventResponse(
                id, ScrapedEventSource.MANUAL_IMPORT.name(), null,
                "Pubquiz", null, null, null, null, null, null,
                ScrapedEventStatus.PROMOTED.name(), null, OffsetDateTime.now(),
                UUID.randomUUID(), promotedEventId, OffsetDateTime.now()
        );
        when(scrapedEventService.promote(eq(id), any(), eq("admin@test.com"))).thenReturn(promoted);

        var body = new PromoteScrapedEventRequest(
                UUID.randomUUID(), UUID.randomUUID(),
                OffsetDateTime.parse("2026-07-01T17:00:00Z"), null, null
        );
        mockMvc.perform(post("/api/v1/admin/scraped-events/" + id + "/promote")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PROMOTED"))
                .andExpect(jsonPath("$.promotedEventId").value(promotedEventId.toString()));
    }

    @Test
    @WithMockUser(roles = "ADMIN", username = "admin@test.com")
    void promote_missingVenueId_returns400() throws Exception {
        UUID id = UUID.randomUUID();
        // venueId and categoryId are @NotNull — omit both
        var body = """
                {"startsAt":"2026-07-01T17:00:00Z"}
                """;

        mockMvc.perform(post("/api/v1/admin/scraped-events/" + id + "/promote")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}
