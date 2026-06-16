package com.umeaevents.scraping;

import com.umeaevents.common.exception.ResourceNotFoundException;
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

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = WebEnvironment.MOCK)
class AdminScrapeSourceControllerTest {

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private ScrapeSourceService service;

    private MockMvc mockMvc;

    private static final String BODY =
            "{\"name\":\"O'Learys\",\"sitemapUrl\":\"https://x/s.xml\",\"urlPattern\":\"/events/.+\"}";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private ScrapeSourceResponse response() {
        return new ScrapeSourceResponse(UUID.randomUUID(), "O'Learys", "https://x/s.xml",
                "/events/.+", true, null, null, null, OffsetDateTime.now());
    }

    // ── auth ──────────────────────────────────────────────────────────────────

    @Test
    void list_noAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/scrape-sources")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void create_userRole_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/admin/scrape-sources")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());
    }

    // ── validation ──────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_missingFields_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/admin/scrape-sources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"x\"}"))
                .andExpect(status().isBadRequest());
        verify(service, never()).create(any());
    }

    // ── CRUD ──────────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void list_admin_returns200() throws Exception {
        when(service.list()).thenReturn(List.of(response()));

        mockMvc.perform(get("/api/v1/admin/scrape-sources"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("O'Learys"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_admin_returns201() throws Exception {
        when(service.create(any())).thenReturn(response());

        mockMvc.perform(post("/api/v1/admin/scrape-sources")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("O'Learys"))
                .andExpect(jsonPath("$.enabled").value(true));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void delete_admin_returns204() throws Exception {
        var id = UUID.randomUUID();
        mockMvc.perform(delete("/api/v1/admin/scrape-sources/{id}", id))
                .andExpect(status().isNoContent());
        verify(service).delete(id);
    }

    // ── run now ──────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void run_admin_returns200() throws Exception {
        var id = UUID.randomUUID();
        when(service.runNow(id)).thenReturn(response());

        mockMvc.perform(post("/api/v1/admin/scrape-sources/{id}/run", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("O'Learys"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void run_missingSource_returns404() throws Exception {
        var id = UUID.randomUUID();
        when(service.runNow(id)).thenThrow(new ResourceNotFoundException("Scrape source not found"));

        mockMvc.perform(post("/api/v1/admin/scrape-sources/{id}/run", id))
                .andExpect(status().isNotFound());
    }
}
