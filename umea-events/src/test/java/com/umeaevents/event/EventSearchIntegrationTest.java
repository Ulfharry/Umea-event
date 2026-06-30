package com.umeaevents.event;

import com.umeaevents.category.Category;
import com.umeaevents.category.CategoryRepository;
import com.umeaevents.event.dto.EventOccurrenceResponse;
import com.umeaevents.user.Role;
import com.umeaevents.user.User;
import com.umeaevents.user.UserRepository;
import com.umeaevents.venue.Venue;
import com.umeaevents.venue.VenueType;
import com.umeaevents.venue.VenueRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the public search query against a real Postgres (native query + interface projection
 * + mapper). The controller test mocks the service, so this is the layer that catches issues like
 * the Instant→OffsetDateTime projection failure that only triggers when rows are returned.
 */
@SpringBootTest
@Transactional
class EventSearchIntegrationTest {

    @Autowired private EventService eventService;
    @Autowired private UserRepository userRepository;
    @Autowired private VenueRepository venueRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private EventRepository eventRepository;
    @Autowired private EventOccurrenceRepository occurrenceRepository;

    @Test
    void search_returnsPublishedOccurrenceWithMappedTimestamps() {
        var title = "IntegrationsQuiz-" + UUID.randomUUID();

        var owner = userRepository.save(User.builder()
                .email("owner-" + UUID.randomUUID() + "@test.com")
                .passwordHash("x")
                .role(Role.RESTAURANT)
                .build());
        Category category = categoryRepository.findAll().get(0);
        var venue = venueRepository.save(Venue.builder()
                .name("Testlokal").type(VenueType.PUB).owner(owner).build());
        var event = eventRepository.save(Event.builder()
                .title(title).venue(venue).category(category).owner(owner)
                .status(EventStatus.PUBLISHED).build());
        var startsAt = OffsetDateTime.now().plusDays(1);
        occurrenceRepository.saveAndFlush(EventOccurrence.builder()
                .event(event).startsAt(startsAt).build());

        var page = eventService.search(null, null, null, null, null, PageRequest.of(0, 50));

        EventOccurrenceResponse found = page.getContent().stream()
                .filter(o -> title.equals(o.title()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("published occurrence not returned by search"));

        assertThat(found.startsAt()).isNotNull();
        assertThat(found.venueName()).isEqualTo("Testlokal");
        assertThat(found.status()).isEqualTo(EventStatus.PUBLISHED);
    }
}
