package com.umeaevents.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.umeaevents.event.dto.CreateEventRequest;
import com.umeaevents.event.dto.EventOccurrenceResponse;
import com.umeaevents.event.dto.EventResponse;
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
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = WebEnvironment.MOCK)
class EventControllerTest {

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private EventService eventService;

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

    private EventOccurrenceResponse sampleOccurrence() {
        return new EventOccurrenceResponse(
                UUID.randomUUID(), UUID.randomUUID(),
                "Pubquiz på Bishops", "Veckans pubquiz",
                "https://img.test/quiz.jpg",
                UUID.randomUUID(), "Bishops Arms",
                UUID.randomUUID(), "Pubquiz",
                EventStatus.PUBLISHED,
                OffsetDateTime.now().plusDays(1),
                OffsetDateTime.now().plusDays(1).plusHours(3),
                OffsetDateTime.now()
        );
    }

    private EventResponse sampleEvent() {
        return new EventResponse(
                UUID.randomUUID(), "Pubquiz på Bishops", "Veckans pubquiz",
                "https://img.test/quiz.jpg",
                UUID.randomUUID(), "Bishops Arms",
                UUID.randomUUID(), "Pubquiz",
                EventStatus.DRAFT,
                UUID.randomUUID(),
                OffsetDateTime.now(), OffsetDateTime.now()
        );
    }

    @Test
    void list_noAuth_returns200() throws Exception {
        var page = new PageImpl<>(List.of(sampleOccurrence()), PageRequest.of(0, 20), 1);
        when(eventService.search(any(), any(), any(), any(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Pubquiz på Bishops"))
                .andExpect(jsonPath("$.content[0].imageUrl").value("https://img.test/quiz.jpg"));
    }

    @Test
    void list_withQueryParam_returns200() throws Exception {
        var page = new PageImpl<>(List.of(sampleOccurrence()), PageRequest.of(0, 20), 1);
        when(eventService.search(any(), any(), any(), any(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/events").param("q", "pubquiz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Pubquiz på Bishops"));
    }

    @Test
    void list_withCategoryFilter_returns200() throws Exception {
        var page = new PageImpl<>(List.of(sampleOccurrence()), PageRequest.of(0, 20), 1);
        when(eventService.search(any(), any(), any(), any(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/events")
                        .param("categoryId", UUID.randomUUID().toString()))
                .andExpect(status().isOk());
    }

    @Test
    void getById_noAuth_returns200() throws Exception {
        var occ = sampleOccurrence();
        when(eventService.getOccurrenceById(occ.id())).thenReturn(occ);

        mockMvc.perform(get("/api/v1/events/" + occ.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Pubquiz på Bishops"));
    }

    @Test
    void create_withoutAuth_returns401() throws Exception {
        var request = new CreateEventRequest(
                "Test", null, null, UUID.randomUUID(), UUID.randomUUID(),
                OffsetDateTime.now().plusDays(1), null);

        mockMvc.perform(post("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void create_withUserRole_returns403() throws Exception {
        var request = new CreateEventRequest(
                "Test", null, null, UUID.randomUUID(), UUID.randomUUID(),
                OffsetDateTime.now().plusDays(1), null);

        mockMvc.perform(post("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "RESTAURANT", username = "owner@test.com")
    void create_withRestaurantRole_returns201() throws Exception {
        var request = new CreateEventRequest(
                "Pubquiz på Bishops", "Veckans pubquiz", null,
                UUID.randomUUID(), UUID.randomUUID(),
                OffsetDateTime.now().plusDays(1), null);
        when(eventService.create(any(), any())).thenReturn(sampleEvent());

        mockMvc.perform(post("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Pubquiz på Bishops"));
    }

    @Test
    @WithMockUser(roles = "RESTAURANT", username = "owner@test.com")
    void submit_asOwner_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        var submitted = new EventResponse(
                id, "Test", null, null, UUID.randomUUID(), "Venue",
                UUID.randomUUID(), "Kategori",
                EventStatus.PENDING_REVIEW, UUID.randomUUID(),
                OffsetDateTime.now(), OffsetDateTime.now());
        when(eventService.submit(any(), any())).thenReturn(submitted);

        mockMvc.perform(post("/api/v1/events/" + id + "/submit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING_REVIEW"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void publish_asAdmin_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        var published = new EventResponse(
                id, "Test", null, null, UUID.randomUUID(), "Venue",
                UUID.randomUUID(), "Kategori",
                EventStatus.PUBLISHED, UUID.randomUUID(),
                OffsetDateTime.now(), OffsetDateTime.now());
        when(eventService.publish(any())).thenReturn(published);

        mockMvc.perform(post("/api/v1/events/" + id + "/publish"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));
    }

    @Test
    @WithMockUser(roles = "RESTAURANT")
    void publish_asRestaurant_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/events/" + UUID.randomUUID() + "/publish"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "RESTAURANT", username = "owner@test.com")
    void cancel_asOwner_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        var cancelled = new EventResponse(
                id, "Test", null, null, UUID.randomUUID(), "Venue",
                UUID.randomUUID(), "Kategori",
                EventStatus.CANCELLED, UUID.randomUUID(),
                OffsetDateTime.now(), OffsetDateTime.now());
        when(eventService.cancel(any(), any())).thenReturn(cancelled);

        mockMvc.perform(post("/api/v1/events/" + id + "/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }
}
