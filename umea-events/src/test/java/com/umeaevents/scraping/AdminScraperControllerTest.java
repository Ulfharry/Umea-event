package com.umeaevents.scraping;

import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = WebEnvironment.MOCK)
class AdminScraperControllerTest {

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private JsoupHtmlScraper scraper;

    @MockitoBean
    private ScrapedEventService scrapedEventService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    private ScrapedEventResponse pendingResponse(String title) {
        return new ScrapedEventResponse(
                UUID.randomUUID(),
                ScrapedEventSource.WEB_SCRAPER.name(),
                null, title, null, null, null, null, null, null,
                ScrapedEventStatus.PENDING_REVIEW.name(),
                null, null, null, null,
                OffsetDateTime.now()
        );
    }

    // ── Auth guards ───────────────────────────────────────────────────────────

    @Test
    void test_noAuth_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/admin/scraper/test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://example.com\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void test_userRole_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/admin/scraper/test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://example.com\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "RESTAURANT")
    void test_restaurantRole_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/admin/scraper/test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://example.com\"}"))
                .andExpect(status().isForbidden());
    }

    // ── Validation ────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void test_missingUrl_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/admin/scraper/test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void test_candidatesFound_returns201WithPendingReviewStatus() throws Exception {
        var candidates = List.of(
                new ScrapeCandidate("Pubquiz", "Rolig kväll", "14 jun", "https://example.com", OffsetDateTime.now()),
                new ScrapeCandidate("Livemusik", null, null, "https://example.com", OffsetDateTime.now())
        );
        var saved = List.of(
                pendingResponse("Pubquiz"),
                pendingResponse("Livemusik")
        );
        when(scraper.scrape("https://example.com")).thenReturn(candidates);
        when(scrapedEventService.saveFromScraper(candidates)).thenReturn(saved);

        mockMvc.perform(post("/api/v1/admin/scraper/test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://example.com\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].rawTitle").value("Pubquiz"))
                .andExpect(jsonPath("$[0].status").value("PENDING_REVIEW"))
                .andExpect(jsonPath("$[0].source").value("WEB_SCRAPER"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void test_noCandidates_returns200WithEmptyList() throws Exception {
        when(scraper.scrape(any())).thenReturn(List.of());

        mockMvc.perform(post("/api/v1/admin/scraper/test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(scrapedEventService, never()).saveFromScraper(any());
    }

    // ── Guardrail: never auto-published ──────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void test_savedResults_areNeverPublished() throws Exception {
        var candidates = List.of(
                new ScrapeCandidate("Event", null, null, "https://example.com", OffsetDateTime.now())
        );
        var saved = List.of(pendingResponse("Event"));
        when(scraper.scrape(any())).thenReturn(candidates);
        when(scrapedEventService.saveFromScraper(any())).thenReturn(saved);

        mockMvc.perform(post("/api/v1/admin/scraper/test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://example.com\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].status").value("PENDING_REVIEW"));
    }

    // ── Error handling ────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void test_unreachableUrl_returns502() throws Exception {
        when(scraper.scrape(any())).thenThrow(new IOException("Connection refused"));

        mockMvc.perform(post("/api/v1/admin/scraper/test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://unreachable.example.com\"}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value("Could not fetch URL: Connection refused"));
    }
}
