package com.umeaevents.event;

import com.umeaevents.category.Category;
import com.umeaevents.category.CategoryRepository;
import com.umeaevents.event.dto.CreatePublishedEventRequest;
import com.umeaevents.user.Role;
import com.umeaevents.user.User;
import com.umeaevents.user.UserRepository;
import com.umeaevents.venue.Venue;
import com.umeaevents.venue.VenueType;
import com.umeaevents.venue.VenueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class EventCreatePublishedIntegrationTest {

    @Autowired private EventService eventService;
    @Autowired private UserRepository userRepository;
    @Autowired private VenueRepository venueRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private EventRepository eventRepository;
    @Autowired private EventOccurrenceRepository occurrenceRepository;
    @Autowired private RecurrenceRuleRepository recurrenceRuleRepository;

    private String adminEmail;
    private Venue venue;
    private Category category;

    @BeforeEach
    void setUp() {
        adminEmail = "admin-" + UUID.randomUUID() + "@test.com";
        var admin = userRepository.save(User.builder().email(adminEmail).passwordHash("x").role(Role.ADMIN).build());
        category = categoryRepository.findAll().get(0);
        venue = venueRepository.save(Venue.builder().name("Lokal").type(VenueType.PUB).owner(admin).build());
    }

    @Test
    void createPublished_single_isPublishedWithOccurrence() {
        var resp = eventService.createPublished(new CreatePublishedEventRequest(
                "Direkt-event", "d", null, venue.getId(), category.getId(),
                OffsetDateTime.parse("2026-09-01T20:00:00Z"), null, null), adminEmail);

        assertThat(resp.status()).isEqualTo(EventStatus.PUBLISHED);
        var event = eventRepository.findById(resp.id()).orElseThrow();
        assertThat(occurrenceRepository.findFirstByEventOrderByStartsAtAsc(event)).isPresent();
    }

    @Test
    void createPublished_recurring_materialisesSeries() {
        long occBefore = occurrenceRepository.count();
        var resp = eventService.createPublished(new CreatePublishedEventRequest(
                "Direkt-serie", null, null, venue.getId(), category.getId(), null, null,
                new CreatePublishedEventRequest.Recurrence(
                        "FREQ=WEEKLY;BYDAY=WE", LocalTime.of(20, 0), 120, "Europe/Stockholm")), adminEmail);

        assertThat(resp.status()).isEqualTo(EventStatus.PUBLISHED);
        var event = eventRepository.findById(resp.id()).orElseThrow();
        assertThat(recurrenceRuleRepository.findByEvent(event)).isPresent();
        assertThat(occurrenceRepository.count()).isGreaterThan(occBefore);
    }
}
