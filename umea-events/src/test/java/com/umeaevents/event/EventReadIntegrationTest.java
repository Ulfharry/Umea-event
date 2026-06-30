package com.umeaevents.event;

import com.umeaevents.category.Category;
import com.umeaevents.category.CategoryRepository;
import com.umeaevents.user.Role;
import com.umeaevents.user.User;
import com.umeaevents.user.UserRepository;
import com.umeaevents.venue.Venue;
import com.umeaevents.venue.VenueType;
import com.umeaevents.venue.VenueRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Read paths that map JPA entities (event → venue/category, which are lazy) MUST run inside a
 * transaction, or the mapper hits LazyInitializationException once the session has closed.
 * This test is intentionally NOT @Transactional so the service methods manage their own
 * transaction exactly like a real request — that's the only way to catch the lazy-init bug.
 */
@SpringBootTest
class EventReadIntegrationTest {

    @Autowired private EventService eventService;
    @Autowired private UserRepository userRepository;
    @Autowired private VenueRepository venueRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private EventRepository eventRepository;
    @Autowired private EventOccurrenceRepository occurrenceRepository;

    private UUID occId, eventId, venueId, userId;
    private String ownerEmail;

    @BeforeEach
    void setUp() {
        ownerEmail = "owner-" + UUID.randomUUID() + "@test.com";
        var owner = userRepository.save(User.builder()
                .email(ownerEmail).passwordHash("x").role(Role.RESTAURANT).build());
        userId = owner.getId();
        Category category = categoryRepository.findAll().get(0);
        var venue = venueRepository.save(Venue.builder()
                .name("Testlokal").type(VenueType.PUB).owner(owner).build());
        venueId = venue.getId();
        var event = eventRepository.save(Event.builder()
                .title("Detalj-event").venue(venue).category(category).owner(owner)
                .status(EventStatus.PUBLISHED).build());
        eventId = event.getId();
        var occ = occurrenceRepository.save(EventOccurrence.builder()
                .event(event).startsAt(OffsetDateTime.now().plusDays(1)).build());
        occId = occ.getId();
    }

    @AfterEach
    void cleanUp() {
        if (occId != null) occurrenceRepository.deleteById(occId);
        if (eventId != null) eventRepository.deleteById(eventId);
        if (venueId != null) venueRepository.deleteById(venueId);
        if (userId != null) userRepository.deleteById(userId);
    }

    @Test
    void getOccurrenceById_mapsLazyAssociationsWithoutError() {
        var response = eventService.getOccurrenceById(occId);

        assertThat(response.title()).isEqualTo("Detalj-event");
        assertThat(response.venueName()).isEqualTo("Testlokal"); // requires lazy venue to load
        assertThat(response.categoryName()).isNotBlank();
        assertThat(response.startsAt()).isNotNull();
    }

    @Test
    void listMine_mapsOwnerEventsWithoutError() {
        var page = eventService.listMine(ownerEmail, null, PageRequest.of(0, 10));

        assertThat(page.getContent())
                .anyMatch(e -> "Detalj-event".equals(e.title()) && "Testlokal".equals(e.venueName()));
    }
}
