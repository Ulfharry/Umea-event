package com.umeaevents.venue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umeaevents.venue.dto.CreateVenueRequest;
import com.umeaevents.venue.dto.VenueResponse;
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
class VenueControllerTest {

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private VenueService venueService;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    private VenueResponse sampleVenue() {
        return new VenueResponse(
                UUID.randomUUID(), "Bishops Arms", "Engelsk pub", VenueType.PUB,
                "Rådhusesplanaden 17", UUID.randomUUID(), true, OffsetDateTime.now());
    }

    @Test
    void list_noAuth_returns200() throws Exception {
        var page = new PageImpl<>(List.of(sampleVenue()), PageRequest.of(0, 20), 1);
        when(venueService.listActive(any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/venues"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Bishops Arms"));
    }

    @Test
    void getById_noAuth_returns200() throws Exception {
        var venue = sampleVenue();
        when(venueService.getById(venue.id())).thenReturn(venue);

        mockMvc.perform(get("/api/v1/venues/" + venue.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Bishops Arms"));
    }

    @Test
    void create_withoutAuth_returns401() throws Exception {
        var request = new CreateVenueRequest("Test", null, VenueType.BAR, null);

        mockMvc.perform(post("/api/v1/venues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "RESTAURANT")
    void create_withRestaurantRole_returns201() throws Exception {
        var request = new CreateVenueRequest("Bishops Arms", "Engelsk pub", VenueType.PUB, "Rådhusesplanaden 17");
        when(venueService.create(any(), any())).thenReturn(sampleVenue());

        mockMvc.perform(post("/api/v1/venues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Bishops Arms"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void create_withUserRole_returns403() throws Exception {
        var request = new CreateVenueRequest("Test", null, VenueType.BAR, null);

        mockMvc.perform(post("/api/v1/venues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void listMine_noAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/venues/mine"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "RESTAURANT", username = "owner@test.com")
    void listMine_authenticated_returns200() throws Exception {
        var page = new PageImpl<>(List.of(sampleVenue()), PageRequest.of(0, 20), 1);
        when(venueService.listMine(eq("owner@test.com"), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/venues/mine"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Bishops Arms"));
    }

    @Test
    @WithMockUser(roles = "RESTAURANT", username = "owner@test.com")
    void delete_asOwner_returns204() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/venues/" + id))
                .andExpect(status().isNoContent());
    }
}
